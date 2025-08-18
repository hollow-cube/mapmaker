package net.hollowcube.terraform.compat.axiom.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMaps;
import net.hollowcube.common.util.Either;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.compat.axiom.data.buffers.AxiomBlockBuffer;
import net.hollowcube.compat.axiom.data.buffers.AxiomBlockEntityData;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.util.PaletteUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@NotNullByDefault
public record AxiomTerraformBuffer(
        Long2ObjectMap<AxiomPalette> sections
) implements BlockBuffer {

    public static AxiomTerraformBuffer of(AxiomBlockBuffer buffer) {
        var exceptions = new ArrayList<Exception>();
        var sections = new Long2ObjectArrayMap<AxiomPalette>();

        Long2ObjectMaps.fastForEach(buffer.updates(), entry -> {
            sections.put(entry.getLongKey(), new AxiomPalette(
                    entry.getValue(),
                    unwrapBlockEntities(exceptions, buffer.blockEntities().get(entry.getLongKey()))
            ));
        });

        if (!exceptions.isEmpty()) {
            var exception = new IOException(String.format("Failed to decompress %d block entities", exceptions.size()));
            exceptions.forEach(exception::addSuppressed);
            ExceptionReporter.reportException(exception);
        }

        return new AxiomTerraformBuffer(sections);
    }

    @Override
    public void forEachSection(@NotNull SectionConsumer consumer) {
        Long2ObjectMaps.fastForEach(this.sections, entry -> {
            long key = entry.getLongKey();
            int x = PaletteUtil.unpackX(key);
            int y = PaletteUtil.unpackY(key);
            int z = PaletteUtil.unpackZ(key);
            consumer.accept(x, y, z, entry.getValue());
        });
    }

    @Override
    public long sizeBytes() {
        return this.sections.values().stream().mapToLong(AxiomPalette::sizeBytes).sum();
    }

    private static Short2ObjectMap<CompoundBinaryTag> unwrapBlockEntities(
            List<Exception> exceptions,
            @Nullable Short2ObjectMap<AxiomBlockEntityData> blockEntities
    ) {
        if (blockEntities == null) return Short2ObjectMaps.emptyMap();

        Short2ObjectMap<CompoundBinaryTag> unwrapped = new Short2ObjectArrayMap<>(blockEntities.size());
        Short2ObjectMaps.fastForEach(blockEntities, entry -> {
            try {
                unwrapped.put(entry.getShortKey(), entry.getValue().decompress());
            } catch (IOException e) {
                exceptions.add(e);
            }
        });

        return unwrapped;
    }

    private record AxiomPalette(
            Either<@Nullable Block, @Nullable Block[]> palette,
            Short2ObjectMap<@Nullable CompoundBinaryTag> blockEntities
    ) implements net.hollowcube.terraform.buffer.palette.Palette {

        @Override
        public @Nullable Block get(int x, int y, int z) {
            var blockEntityKey = (short) (x | (y << 4) | (z << 8));
            var blockEntityData = this.blockEntities.get(blockEntityKey);

            var block = this.palette.map(Function.identity(), blocks -> blocks[x + 16 * (z + 16 * y)]);
            if (block == null || blockEntityData == null) return block;
            var blockEntity = OpUtils.map(block.registry().blockEntity(), Key::asString);
            if (blockEntity == null) return block;

            return block.withNbt(blockEntityData).withHandler(MinecraftServer.getBlockManager().getHandler(blockEntity));
        }

        @Override
        public long sizeBytes() {
            return (long) palette.map(
                    _ -> Integer.BYTES,
                    blocks -> blocks.length * Integer.BYTES
            );
        }
    }
}
