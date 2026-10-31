package net.hollowcube.terraform.clipboard;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.Transform;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.hollowcube.terraform.util.BlockUtil.packBlockPos;
import static org.junit.jupiter.api.Assertions.*;

class ClipboardRemapTest {

    @Test
    void rotate90FourTimesRestoresBlocksAndBlockEntities() {
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putByte("Marker", (byte) 9).build();
        EditBuffer source = EditBuffer.builder()
            .setBlockState(0, 70, 0, Block.OAK_STAIRS.withProperty("facing", "east"))
            .setBlockState(1, 70, 0, Block.STONE)
            .setBlockState(3, 70, 0, Block.AIR)
            .setBlockState(0, 70, 2, Block.OAK_LOG.withProperty("axis", "x"))
            .setBlockState(2, 70, 3, Block.OAK_SIGN.withProperty("rotation", "0").withNbt(nbt))
            .build();

        EditBuffer out = source;
        for (int i = 0; i < 4; i++) out = Clipboard.remap(out, Transform.rotateY(90), false);

        assertEquals(snapshot(source), snapshot(out));
    }

    @Test
    void mirrorTwiceIsIdentity() {
        EditBuffer source = EditBuffer.builder()
            .setBlockState(
                0,
                70,
                0,
                Block.OAK_STAIRS.withProperties(Map.of("facing", "east", "shape", "inner_left"))
            )
            .setBlockState(2, 70, 1, Block.OAK_LOG.withProperty("axis", "z"))
            .build();
        Transform mirror = Transform.mirror(Transform.Axis.X);
        EditBuffer once = Clipboard.remap(source, mirror, false);
        EditBuffer twice = Clipboard.remap(once, mirror, false);
        assertEquals(snapshot(source), snapshot(twice));
        assertNotEquals(snapshot(source), snapshot(once));
    }

    @Test
    void rotateMovesAndReorients() {
        EditBuffer source = EditBuffer.builder()
            .setBlockState(2, 70, 0, Block.OAK_STAIRS.withProperty("facing", "east"))
            .build();
        // About the corner of (2, 70, 0): centre (2.5, _, 0.5) turns to (1.5, _, 0.5).
        EditBuffer out = Clipboard.remap(
            source,
            about(new Vec(2, 70, 0), Transform.rotateY(90)),
            false
        );
        assertEquals(EditBuffer.NONE, out.getBlockState(2, 70, 0));
        assertEquals(
            Block.OAK_STAIRS.withProperty("facing", "south").stateId(),
            out.getBlockState(1, 70, 0)
        );
    }

    @Test
    void translationMovesCellsAcrossSections() {
        EditBuffer source = EditBuffer.builder()
            .setBlockState(15, 70, 15, Block.STONE)
            .setBlockState(-1, 70, 0, Block.DIRT)
            .build();
        EditBuffer out = Clipboard.remap(source, Transform.translate(1, -71, 3), false);
        assertEquals(Block.STONE.stateId(), out.getBlockState(16, -1, 18));
        assertEquals(Block.DIRT.stateId(), out.getBlockState(0, -1, 3));
        assertEquals(2, countSet(out));
    }

    @Test
    void skipAirDropsAir() {
        EditBuffer source = EditBuffer.builder()
            .setBlockState(0, 70, 0, Block.STONE)
            .setBlockState(1, 70, 0, Block.AIR)
            .build();
        assertEquals(1, countSet(Clipboard.remap(source, Transform.rotateY(90), true)));
        assertEquals(2, countSet(Clipboard.remap(source, Transform.rotateY(90), false)));
    }

    @Test
    void blockEntityKeepsNbtAndHandlerAndReorients() {
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putString("Text", "hi").build();
        BlockHandler handler = BlockHandler.Dummy.get("test:sign");
        EditBuffer source = EditBuffer.builder()
            .setBlockState(
                0,
                70,
                0,
                Block.OAK_SIGN.withProperty("rotation", "0").withNbt(nbt).withHandler(handler)
            )
            .build();

        EditBuffer out = Clipboard.remap(source, Transform.rotateY(90), false);

        // The centre (0.5, _, 0.5) turns to (-0.5, _, 0.5).
        Block moved = Objects.requireNonNull(out.getBlock(-1, 70, 0));
        assertEquals("4", moved.getProperty("rotation"));
        assertEquals(nbt, moved.nbt());
        assertSame(handler, moved.handler());
    }

    @Test
    void biomeRoundTrips() {
        EditBuffer source = EditBuffer.builder().setBiome(0, 70, 0, 5).build();
        EditBuffer out = source;
        for (int i = 0; i < 4; i++) out = Clipboard.remap(out, Transform.rotateY(90), false);
        assertEquals(5, out.getBiome(0, 70, 0));
    }

    @Test
    void mirrorCarriesBiomesWithTheBlocks() {
        // Biome cell (0..3, 68..71, 0..3) mirrors to (-4..-1, 68..71, 0..3).
        EditBuffer source = EditBuffer.builder()
            .setBlockState(1, 70, 1, Block.STONE)
            .setBiome(1, 70, 1, 5)
            .build();
        EditBuffer out = Clipboard.remap(source, Transform.mirror(Transform.Axis.X), false);
        assertEquals(Block.STONE.stateId(), out.getBlockState(-2, 70, 1));
        assertEquals(5, out.getBiome(-2, 70, 1));
        assertEquals(EditBuffer.NONE, out.getBiome(1, 70, 1));
    }

    @Test
    void uniformSectionMovesWhole() {
        EditBuffer source = EditBuffer.builder()
            .fillSection(0, 4, 0, Block.STONE.stateId())
            .build();
        EditBuffer out = Clipboard.remap(source, Transform.rotateY(90), false);
        assertEquals(4096, countSet(out));
        assertEquals(
            Area.cuboid(new BlockVec(-16, 64, 0), new BlockVec(-1, 79, 15)),
            out.blockBounds()
        );

        EditBuffer air = EditBuffer.builder().fillSection(0, 4, 0, Block.AIR.stateId()).build();
        assertEquals(0, countSet(Clipboard.remap(air, Transform.rotateY(90), true)));
    }

    @Test
    void emptyBufferStaysEmpty() {
        assertEquals(
            0,
            Clipboard.remap(EditBuffer.empty(), Transform.rotateY(90), false).sectionCount()
        );
    }

    @Test
    void rejectsTransformsThatAreNotCubeSymmetries() {
        EditBuffer source = EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE).build();
        assertThrows(
            IllegalArgumentException.class,
            () -> Clipboard.remap(source, Transform.rotateY(45), false)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Clipboard.remap(source, new Transform(2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0), false)
        );
    }

    /// `t` about the block corner `p`.
    private static Transform about(Point p, Transform t) {
        return Transform.translate(p.x(), p.y(), p.z())
            .compose(t)
            .compose(Transform.translate(-p.x(), -p.y(), -p.z()));
    }

    private static Map<Long, Cell> snapshot(EditBuffer buffer) {
        Map<Long, Cell> cells = new HashMap<>();
        buffer.forEachSection((sx, sy, sz, section) -> {
            int baseX = sx << 4, baseY = sy << 4, baseZ = sz << 4;
            section.forEachBlockState((lx, ly, lz, stateId) -> {
                Block be = section.getBlockEntity(lx, ly, lz);
                cells.put(
                    packBlockPos(baseX + lx, baseY + ly, baseZ + lz),
                    new Cell(stateId, be == null ? null : be.nbt())
                );
            });
        });
        return cells;
    }

    private static long countSet(EditBuffer buffer) {
        long[] n = {0};
        buffer.forEachSection((_, _, _, section) -> n[0] += section.count());
        return n[0];
    }

    private record Cell(int stateId, @Nullable CompoundBinaryTag nbt) {}
}
