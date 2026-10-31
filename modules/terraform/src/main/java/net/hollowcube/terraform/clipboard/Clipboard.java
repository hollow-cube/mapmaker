package net.hollowcube.terraform.clipboard;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.util.Transform;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/// A [Schematic] held for pasting, with a pending [Transform] from rotates and flips. Transforms compose
/// exactly and are applied once, at paste, pivoting on the centre of the origin block, so a paste always
/// lands the origin block on the anchor block.
@SuppressWarnings("UnstableApiUsage")
public final class Clipboard {
    private final Schematic schematic;
    private final Transform transform;

    public Clipboard(Schematic schematic) {
        this(schematic, Transform.identity());
    }

    public Clipboard(EditBuffer buffer, Point origin) {
        this(new Schematic(buffer, origin.asBlockVec()));
    }

    private Clipboard(Schematic schematic, Transform transform) {
        this.schematic = schematic;
        this.transform = transform;
    }

    /// The untransformed schematic.
    public Schematic schematic() {
        return schematic;
    }

    /// The untransformed buffer, at the coordinates it was copied from.
    public EditBuffer buffer() {
        return schematic.buffer();
    }

    public BlockVec origin() {
        return schematic.origin();
    }

    public Transform transform() {
        return transform;
    }

    public boolean hasTransform() {
        return !transform.isIdentity();
    }

    @Contract(pure = true)
    public Clipboard withTransform(Transform extra) {
        return new Clipboard(schematic, extra.compose(transform));
    }

    /// The schematic with any pending transform applied, for saving.
    public Schematic toSchematic() {
        if (!hasTransform()) return schematic;
        return new Schematic(pasteBuffer(origin(), false), origin(), schematic.metadata());
    }

    public EditBuffer pasteBuffer(Point anchor, boolean skipAir) {
        return remap(buffer(), pasteTransform(anchor), skipAir);
    }

    public Area.@Nullable Cuboid pasteBounds(Point anchor) {
        Area.Cuboid source = buffer().blockBounds();
        if (source == null) return null;

        Transform paste = pasteTransform(anchor);
        BlockVec min = source.min(), max = source.max();
        return Area.cuboid(
            paste.applyBlock(min.blockX(), min.blockY(), min.blockZ()),
            paste.applyBlock(max.blockX(), max.blockY(), max.blockZ())
        );
    }

    private Transform pasteTransform(Point anchor) {
        return Transform
            .translate(anchor.blockX() + 0.5, anchor.blockY() + 0.5, anchor.blockZ() + 0.5)
            .compose(transform)
            .compose(
                Transform.translate(-origin().x() - 0.5, -origin().y() - 0.5, -origin().z() - 0.5)
            );
    }

    /// Moves every set cell of `source` to the cell holding its transformed centre, reorienting its block
    /// and dropping air when `skipAir`. Block entities and biomes move with their cells.
    ///
    /// @throws IllegalArgumentException unless `transform` is a [cube symmetry][Transform#isCubeSymmetry()]
    ///     (90 degree rotations and flips), the only transforms that map cells one to one
    static EditBuffer remap(EditBuffer source, Transform transform, boolean skipAir) {
        if (transform.isIdentity() && !skipAir) return source;
        if (!transform.isCubeSymmetry()) {
            throw new IllegalArgumentException(
                "only 90 degree rotations and flips are supported: " + transform
            );
        }

        // A 90 degree rotation or flip maps whole coordinates to whole coordinates, so every cell lands at
        // m * p plus the same offset (where the centre shift and translation end up), computed once here.
        int[] m = {
            (int) Math.round(transform.m00()),
            (int) Math.round(transform.m01()),
            (int) Math.round(transform.m02()),
            (int) Math.round(transform.m10()),
            (int) Math.round(transform.m11()),
            (int) Math.round(transform.m12()),
            (int) Math.round(transform.m20()),
            (int) Math.round(transform.m21()),
            (int) Math.round(transform.m22()),
        };

        int cx = (int) Math.floor((m[0] + m[1] + m[2]) * 0.5 + transform.m03());
        int cy = (int) Math.floor((m[3] + m[4] + m[5]) * 0.5 + transform.m13());
        int cz = (int) Math.floor((m[6] + m[7] + m[8]) * 0.5 + transform.m23());

        var blocks = new BlockTransformer(transform);
        var out = EditBuffer.builder();
        source.forEachSection((sx, sy, sz, section) -> {
            int baseX = sx << 4, baseY = sy << 4, baseZ = sz << 4;

            section.forEachBlockState((lx, ly, lz, stateId) -> {
                if (skipAir && stateId == Block.AIR.stateId()) return;
                int x = baseX + lx, y = baseY + ly, z = baseZ + lz;
                int dx = m[0] * x + m[1] * y + m[2] * z + cx;
                int dy = m[3] * x + m[4] * y + m[5] * z + cy;
                int dz = m[6] * x + m[7] * y + m[8] * z + cz;
                Block blockEntity = section.getBlockEntity(lx, ly, lz);
                if (blockEntity != null) {
                    out.setBlockState(dx, dy, dz, blocks.transform(blockEntity));
                } else {
                    out.setBlockState(dx, dy, dz, blocks.transformState(stateId));
                }
            });

            section.forEachBiome((bx, by, bz, biomeId) -> {
                // A biome cell has no orientation: it lands on the biome cell holding its centre.
                double x = baseX + bx * 4 + 2.0, y = baseY + by * 4 + 2.0, z = baseZ + bz * 4 + 2.0;
                out.setBiome(
                    (int) Math.floor(m[0] * x + m[1] * y + m[2] * z + transform.m03()),
                    (int) Math.floor(m[3] * x + m[4] * y + m[5] * z + transform.m13()),
                    (int) Math.floor(m[6] * x + m[7] * y + m[8] * z + transform.m23()),
                    biomeId
                );
            });
        });
        return out.build();
    }
}
