package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.hollowcube.terraform.util.BlockUtil;
import net.hollowcube.terraform.util.PosConsumer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.hollowcube.terraform.util.BlockUtil.*;

/// A one-way edit: absolute, section-addressed, apply-oriented. A sparse map of section key to
/// [BufferSection], randomly addressable. The only entry point to editing.
@SuppressWarnings("UnstableApiUsage")
public final class EditBuffer {
    private static final EditBuffer EMPTY = new EditBuffer(Long2ObjectMaps.emptyMap(), null);

    /// ID for "not set" aka untouched.
    public static final int NONE = BufferSection.NONE;

    public static EditBuffer empty() {
        return EMPTY;
    }

    @Contract("-> new")
    public static Builder builder() {
        return new Builder();
    }

    private final Long2ObjectMap<BufferSection> sections;
    private final Area.@Nullable Cuboid blockBounds;

    private EditBuffer(Long2ObjectMap<BufferSection> sections, Area.@Nullable Cuboid blockBounds) {
        this.sections = sections;
        this.blockBounds = blockBounds;
    }

    public int sectionCount() {
        return sections.size();
    }

    /// LOSSY: state id only, no NBT/block-entity data. Use [#getBlock] for the full block. [#NONE] if untouched.
    public int getBlockState(int x, int y, int z) {
        var section = sectionAt(x, y, z);
        return section == null ? NONE : section.getBlockState(x & 15, y & 15, z & 15);
    }

    /// The full block (incl. nbt/handler), or null when the cell is untouched (not air).
    public @Nullable Block getBlock(int x, int y, int z) {
        var section = sectionAt(x, y, z);
        if (section == null) return null;

        int rx = x & 0xF, ry = y & 0xF, rz = z & 0xF;
        var blockEntity = section.getBlockEntity(rx, ry, rz);
        if (blockEntity != null) return blockEntity;
        int stateId = section.getBlockState(rx, ry, rz);
        return stateId == NONE ? null : Block.fromStateId(stateId);
    }

    /// The set biome id for the biome cell containing this block coordinate, or [#NONE].
    public int getBiome(int x, int y, int z) {
        var section = sectionAt(x, y, z);
        return section == null
            ? NONE
            : section.getBiome((x & 0xF) >> 2, (y & 0xF) >> 2, (z & 0xF) >> 2);
    }

    /// The section at section coordinates (eg block >> 4), or null when the buffer has none there.
    @SuppressWarnings("DataFlowIssue")
    public @Nullable BufferSection section(int sectionX, int sectionY, int sectionZ) {
        return sections.get(packBlockPos(sectionX, sectionY, sectionZ));
    }

    /// The smallest box holding every set block, or null when no block is set. After [#clipToBox] it is the
    /// original box clipped, which can be larger than what the clip kept.
    public Area.@Nullable Cuboid blockBounds() {
        return blockBounds;
    }

    /// Coordinates in the consumer are the section coordinates (eg block >> 4)
    public void forEachSection(PosConsumer<BufferSection> consumer) {
        for (var entry : Long2ObjectMaps.fastIterable(sections)) {
            long key = entry.getLongKey();
            consumer.accept(
                blockPosGetX(key),
                blockPosGetY(key),
                blockPosGetZ(key),
                entry.getValue()
            );
        }
    }

    /// Returns a buffer holding only the content inside the inclusive box of min/maxXYZ.
    public EditBuffer clipToBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean changed = false;
        boolean anyBlocks = false;
        Long2ObjectMap<BufferSection> out = new Long2ObjectOpenHashMap<>(sections.size());
        for (var entry : Long2ObjectMaps.fastIterable(sections)) {
            long key = entry.getLongKey();
            int baseX = blockPosGetX(key) << 4;
            int baseY = blockPosGetY(key) << 4;
            int baseZ = blockPosGetZ(key) << 4;

            boolean outside = baseX + 15 < minX
                || baseX > maxX
                || baseY + 15 < minY
                || baseY > maxY
                || baseZ + 15 < minZ
                || baseZ > maxZ;
            if (outside) {
                changed = true;
                // drop the section
                continue;
            }

            boolean inside = baseX >= minX
                && baseX + 15 <= maxX
                && baseY >= minY
                && baseY + 15 <= maxY
                && baseZ >= minZ
                && baseZ + 15 <= maxZ;
            if (inside) {
                out.put(key, entry.getValue()); // dont need to change
                anyBlocks |= entry.getValue().hasBlocks();
                continue;
            }

            BufferSection copy = entry.getValue().deepCopy();
            boolean hasBlocks = copy.filterBlocks((x, y, z) -> {
                int wx = baseX + x, wy = baseY + y, wz = baseZ + z;
                return wx >= minX
                    && wx <= maxX
                    && wy >= minY
                    && wy <= maxY
                    && wz >= minZ
                    && wz <= maxZ;
            });
            boolean hasBiomes = copy.filterBiomes((x, y, z) -> {
                // A biome cell covers 4×4×4 blocks from its origin and is kept if any of them is in the box.
                int wx = baseX + (x << 2), wy = baseY + (y << 2), wz = baseZ + (z << 2);
                return wx <= maxX
                    && wx + 3 >= minX
                    && wy <= maxY
                    && wy + 3 >= minY
                    && wz <= maxZ
                    && wz + 3 >= minZ;
            });
            changed = true;
            anyBlocks |= hasBlocks;
            if (hasBlocks || hasBiomes) out.put(key, copy);
        }
        if (!changed) return this;

        Area.@Nullable Cuboid bounds = null;
        if (anyBlocks && blockBounds != null) {
            BlockVec low = blockBounds.min().max(minX, minY, minZ);
            BlockVec high = blockBounds.max().min(maxX, maxY, maxZ);
            bounds = Area.cuboid(low, high);
        }
        return new EditBuffer(out, bounds);
    }

    public long sizeBytes() {
        long total = 0;
        for (BufferSection section : sections.values()) {
            total += section.sizeBytes();
        }
        return total;
    }

    // Stupid intellij cant figure out that get is nullable (or stupid fastutil idk)
    @SuppressWarnings("DataFlowIssue")
    private @Nullable BufferSection sectionAt(int x, int y, int z) {
        return sections.get(packBlockPos(x >> 4, y >> 4, z >> 4));
    }

    /// Single-use builder, its invalid after [#build()].
    ///
    /// Not thread-safe
    public static final class Builder {
        private final Long2ObjectMap<BufferSection> sections = new Long2ObjectOpenHashMap<>();

        private boolean built;

        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int maxZ = Integer.MIN_VALUE;

        // Single-entry cache: consecutive sets land in the same section
        private boolean cacheValid;
        private long cacheKey;
        private @Nullable BufferSection cacheSection;

        @Contract("_, _, _, _ -> this")
        public Builder setBlockState(int x, int y, int z, int stateId) {
            BufferSection section = section(x, y, z);
            int rx = x & 15, ry = y & 15, rz = z & 15;
            section.setBlockState(rx, ry, rz, stateId);
            section.removeBlockEntity(rx, ry, rz);
            include(x, y, z, x, y, z);
            return this;
        }

        /// Set a block, capturing NBT/handler when present.
        @Contract("_, _, _, _ -> this")
        public Builder setBlockState(int x, int y, int z, Block block) {
            BufferSection section = section(x, y, z);
            int rx = x & 15, ry = y & 15, rz = z & 15;
            section.setBlockState(rx, ry, rz, block.stateId());
            if (BlockUtil.isPlainState(block)) {
                section.removeBlockEntity(rx, ry, rz);
            } else {
                section.setBlockEntity(rx, ry, rz, block);
            }
            include(x, y, z, x, y, z);
            return this;
        }

        /// The state id set at a cell so far, or [#NONE].
        public int getBlockState(int x, int y, int z) {
            assertNotBuilt();
            BufferSection section = sections.get(packBlockPos(x >> 4, y >> 4, z >> 4));
            //noinspection ConstantValue fastutil get is nullable
            return section == null ? NONE : section.getBlockState(x & 15, y & 15, z & 15);
        }

        @Contract("_, _, _, _ -> this")
        public Builder fillSection(int sectionX, int sectionY, int sectionZ, int stateId) {
            replaceSection(
                packBlockPos(sectionX, sectionY, sectionZ),
                BufferSection.uniform(stateId)
            );
            includeSection(sectionX, sectionY, sectionZ);
            return this;
        }

        @Contract("_, _, _, _ -> this")
        public Builder setBiome(int x, int y, int z, int biomeId) {
            section(x, y, z).setBiome((x & 15) >> 2, (y & 15) >> 2, (z & 15) >> 2, biomeId);
            return this;
        }

        /// Captures a whole world section's blocks (air included) as the builder's section at the key and
        /// returns it read-only (its mutators are not public). Throws if the builder already holds that
        /// section, since capturing would discard what it recorded. Add the section's block entities with
        /// [#captureBlockEntity].
        @ApiStatus.Internal
        public BufferSection captureSection(
            int sectionX,
            int sectionY,
            int sectionZ,
            Palette worldBlocks
        ) {
            assertNotBuilt();
            long key = packBlockPos(sectionX, sectionY, sectionZ);
            if (sections.containsKey(key)) {
                throw new IllegalStateException(
                    "section %d, %d, %d was already captured".formatted(
                        sectionX,
                        sectionY,
                        sectionZ
                    )
                );
            }
            var section = new BufferSection(PaletteHack.copyWithOffset(worldBlocks));
            replaceSection(key, section);
            includeSection(sectionX, sectionY, sectionZ);
            return section;
        }

        /// Records block-entity data at a cell of a section the builder already holds (one from
        /// [#captureSection]), leaving the cell's block state alone. Throws if no section holds the cell.
        @ApiStatus.Internal
        public void captureBlockEntity(int x, int y, int z, Block block) {
            assertNotBuilt();
            BufferSection section = sections.get(packBlockPos(x >> 4, y >> 4, z >> 4));
            //noinspection ConstantValue fastutil get is nullable
            if (section == null) {
                throw new IllegalStateException("no section holds " + x + ", " + y + ", " + z);
            }
            section.setBlockEntity(x & 15, y & 15, z & 15, block);
        }

        @Contract(" -> new")
        public EditBuffer build() {
            assertNotBuilt();
            built = true;
            Area.@Nullable Cuboid bounds = minX > maxX
                ? null
                : Area.cuboid(new BlockVec(minX, minY, minZ), new BlockVec(maxX, maxY, maxZ));
            return new EditBuffer(sections, bounds);
        }

        private void replaceSection(long key, BufferSection section) {
            assertNotBuilt();
            sections.put(key, section);
            cacheValid = false; // a put may replace the cached section
        }

        private BufferSection section(int x, int y, int z) {
            assertNotBuilt();
            long key = packBlockPos(x >> 4, y >> 4, z >> 4);
            if (cacheValid && key == cacheKey) {
                return Objects.requireNonNull(cacheSection, "cached section (cacheValid)");
            }
            BufferSection section = sections.computeIfAbsent(key, _ -> new BufferSection());

            cacheKey = key;
            cacheSection = section;
            cacheValid = true;
            return section;
        }

        private void includeSection(int sectionX, int sectionY, int sectionZ) {
            int x = sectionX << 4, y = sectionY << 4, z = sectionZ << 4;
            include(x, y, z, x + 15, y + 15, z + 15);
        }

        private void include(int lowX, int lowY, int lowZ, int highX, int highY, int highZ) {
            minX = Math.min(minX, lowX);
            minY = Math.min(minY, lowY);
            minZ = Math.min(minZ, lowZ);
            maxX = Math.max(maxX, highX);
            maxY = Math.max(maxY, highY);
            maxZ = Math.max(maxZ, highZ);
        }

        private void assertNotBuilt() {
            Check.stateCondition(built, "builder already built");
        }

        private Builder() {}
    }
}
