package net.hollowcube.terraform.clipboard;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.schem.SpongeSchematicReader;
import net.hollowcube.terraform.schem.SpongeSchematicWriter;
import net.hollowcube.terraform.util.GameDataProvider;
import net.hollowcube.terraform.util.Transform;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ClipboardSchematicTest {
    private static final Vec ANCHOR = new Vec(100, 50, -100);

    static {
        MinecraftServer.init();
    }

    @Test
    void untransformedClipboardIsItsSchematic() {
        EditBuffer buffer = EditBuffer.builder().setBlockState(1, 2, 3, Block.STONE).build();
        Clipboard clip = new Clipboard(buffer, new Vec(1, 2, 3));
        assertSame(clip.schematic(), clip.toSchematic());
    }

    @Test
    void rotatedClipboardSavesBaked() throws IOException {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(5, 70, 5, Block.OAK_STAIRS.withProperty("facing", "east"))
            .setBlockState(8, 70, 5, Block.OAK_LOG.withProperty("axis", "x"))
            .setBlockState(
                6,
                72,
                9,
                Block.CHEST.withProperty("facing", "north")
                    .withNbt(CompoundBinaryTag.builder().putString("Lock", "k").build())
            )
            .build();
        Clipboard clip = new Clipboard(buffer, new Vec(6, 70, 6))
            .withTransform(Transform.rotateY(90));
        var encoded = SpongeSchematicWriter.write(clip.toSchematic(), GameDataProvider.NOOP);
        Clipboard read = new Clipboard(SpongeSchematicReader.read(encoded, GameDataProvider.NOOP));

        assertFalse(read.hasTransform());
        assertPastesMatch(clip, read);
        Block chest = Objects.requireNonNull(
            read.pasteBuffer(ANCHOR, false).getBlock(97, 52, -100)
        );
        assertEquals("east", chest.getProperty("facing"));
        assertEquals("k", Objects.requireNonNull(chest.nbt()).getString("Lock"));
    }

    @Test
    void mirroredClipboardSavesBaked() throws IOException {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(
                -2,
                0,
                0,
                Block.OAK_STAIRS.withProperties(Map.of("facing", "east", "shape", "inner_left"))
            )
            .setBlockState(1, 3, -4, Block.STONE)
            .build();
        Clipboard clip = new Clipboard(buffer, Vec.ZERO)
            .withTransform(Transform.mirror(Transform.Axis.X))
            .withTransform(Transform.rotateX(90));
        assertPastesMatch(clip, roundTrip(clip));
    }

    private static Clipboard roundTrip(Clipboard clipboard) throws IOException {
        var encoded = SpongeSchematicWriter.write(clipboard.toSchematic(), GameDataProvider.NOOP);
        return new Clipboard(SpongeSchematicReader.read(encoded, GameDataProvider.NOOP));
    }

    /// Every cell `expected` pastes, `actual` pastes the same at the same anchor. `actual` may add air
    /// for the cells `expected` left unset.
    private static void assertPastesMatch(Clipboard expected, Clipboard actual) {
        EditBuffer want = expected.pasteBuffer(ANCHOR, false);
        EditBuffer got = actual.pasteBuffer(ANCHOR, false);
        assertEquals(want.blockBounds(), got.blockBounds());
        want.forEachSection(
            (sx, sy, sz, section) -> section.forEachBlockState((x, y, z, stateId) -> {
                int ax = (sx << 4) + x, ay = (sy << 4) + y, az = (sz << 4) + z;
                assertEquals(stateId, got.getBlockState(ax, ay, az), ax + ", " + ay + ", " + az);
                assertEquals(want.getBlock(ax, ay, az), got.getBlock(ax, ay, az));
            })
        );
        got.forEachSection(
            (sx, sy, sz, section) -> section.forEachBlockState((x, y, z, stateId) -> {
                int ax = (sx << 4) + x, ay = (sy << 4) + y, az = (sz << 4) + z;
                if (want.getBlockState(ax, ay, az) == EditBuffer.NONE) {
                    assertEquals(Block.AIR.stateId(), stateId, ax + ", " + ay + ", " + az);
                }
            })
        );
    }
}
