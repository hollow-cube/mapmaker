package net.hollowcube.terraform.clipboard;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.Transform;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClipboardTest {

    @Test
    void pasteShiftsCells() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 70, 0, Block.STONE)
            .setBlockState(2, 70, 2, Block.DIRT)
            .build();
        Clipboard clip = new Clipboard(buffer, new Vec(0, 70, 0));

        EditBuffer pasted = clip.pasteBuffer(new Vec(10, 70, 10), false);
        assertEquals(Block.STONE.stateId(), pasted.getBlockState(10, 70, 10));
        assertEquals(Block.DIRT.stateId(), pasted.getBlockState(12, 70, 12));
        assertEquals(EditBuffer.NONE, pasted.getBlockState(0, 70, 0));
    }

    @Test
    void pasteCarriesBiomesAndFloorsTheOrigin() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 64, 0, Block.STONE)
            .setBiome(0, 64, 0, 7)
            .build();
        Clipboard clip = new Clipboard(buffer, new Vec(0.7, 64.2, 0.9));
        assertEquals(new BlockVec(0, 64, 0), clip.origin());

        EditBuffer pasted = clip.pasteBuffer(new Vec(16, 64, 0), false);
        assertEquals(Block.STONE.stateId(), pasted.getBlockState(16, 64, 0));
        assertEquals(7, pasted.getBiome(16, 64, 0));
    }

    @Test
    void anchorIsFloored() {
        Clipboard clip = new Clipboard(
            EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE).build(),
            Vec.ZERO
        );
        EditBuffer pasted = clip.pasteBuffer(new Vec(-0.5, 3.9, 2.1), false);
        assertEquals(Block.STONE.stateId(), pasted.getBlockState(-1, 3, 2));
    }

    @Test
    void skipAirDropsAirCells() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 70, 0, Block.STONE)
            .setBlockState(1, 70, 0, Block.AIR)
            .build();
        Clipboard clip = new Clipboard(buffer, new Vec(0, 70, 0));

        Vec anchor = new Vec(0, 70, 0);
        EditBuffer withAir = clip.pasteBuffer(anchor, false);
        assertEquals(Block.AIR.stateId(), withAir.getBlockState(1, 70, 0));

        EditBuffer noAir = clip.pasteBuffer(anchor, true);
        assertEquals(Block.STONE.stateId(), noAir.getBlockState(0, 70, 0));
        assertEquals(EditBuffer.NONE, noAir.getBlockState(1, 70, 0));
    }

    @Test
    void everyRotationAndFlipLandsTheOriginCellOnTheAnchor() {
        // Off the section grid, so a corner pivot would visibly shift the origin cell.
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(5, 70, 5, Block.STONE)
            .setBlockState(6, 70, 5, Block.DIRT)
            .build();
        var clip = new Clipboard(buffer, new Vec(5, 70, 5));
        var anchor = new Vec(20, 64, 30);
        var transformed = List.of(
            clip.withTransform(Transform.rotateY(90)),
            clip.withTransform(Transform.rotateY(180)),
            clip.withTransform(Transform.rotateY(270)),
            clip.withTransform(Transform.rotateX(90)),
            clip.withTransform(Transform.mirror(Transform.Axis.X)),
            clip.withTransform(Transform.mirror(Transform.Axis.Y)),
            clip.withTransform(Transform.mirror(Transform.Axis.Z))
        );
        for (Clipboard t : transformed) {
            EditBuffer pasted = t.pasteBuffer(anchor, false);
            assertEquals(
                Block.STONE.stateId(),
                pasted.getBlockState(20, 64, 30),
                "origin cell under " + t.transform()
            );
        }

        EditBuffer rotated = clip.withTransform(Transform.rotateY(90)).pasteBuffer(anchor, false);
        assertEquals(Block.DIRT.stateId(), rotated.getBlockState(20, 64, 31));
        EditBuffer mirrored = clip.withTransform(Transform.mirror(Transform.Axis.X))
            .pasteBuffer(anchor, false);
        assertEquals(Block.DIRT.stateId(), mirrored.getBlockState(19, 64, 30));
    }

    @Test
    void transformsComposeInOrder() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(0, 0, 0, Block.STONE)
            .setBlockState(1, 0, 0, Block.DIRT)
            .build();
        Clipboard clip = new Clipboard(buffer, Vec.ZERO);

        // +x turns to +z, then the z mirror sends it to -z.
        EditBuffer rotateThenMirror = clip.withTransform(Transform.rotateY(90))
            .withTransform(Transform.mirror(Transform.Axis.Z))
            .pasteBuffer(Vec.ZERO, false);
        assertEquals(Block.DIRT.stateId(), rotateThenMirror.getBlockState(0, 0, -1));

        // The z mirror leaves +x alone, then it turns to +z.
        EditBuffer mirrorThenRotate = clip.withTransform(Transform.mirror(Transform.Axis.Z))
            .withTransform(Transform.rotateY(90))
            .pasteBuffer(Vec.ZERO, false);
        assertEquals(Block.DIRT.stateId(), mirrorThenRotate.getBlockState(0, 0, 1));
    }

    @Test
    void transformIsDeferredAndLeavesTheSourceAlone() {
        EditBuffer buffer = EditBuffer.builder().setBlockState(1, 0, 0, Block.STONE).build();
        Clipboard clip = new Clipboard(buffer, Vec.ZERO);
        Clipboard rotated = clip.withTransform(Transform.rotateY(90));

        assertFalse(clip.hasTransform());
        assertTrue(rotated.hasTransform());
        assertSame(buffer, rotated.buffer());
        assertEquals(clip.buffer().blockBounds(), rotated.buffer().blockBounds());
        assertFalse(clip.withTransform(Transform.rotateY(360)).hasTransform());
    }

    @Test
    void sourceBoundsHoldEveryBlock() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(-3, 10, 4, Block.STONE)
            .setBlockState(20, 12, -1, Block.AIR)
            .build();
        Clipboard clip = new Clipboard(buffer, Vec.ZERO);
        assertEquals(
            Area.cuboid(new BlockVec(-3, 10, -1), new BlockVec(20, 12, 4)),
            clip.buffer().blockBounds()
        );
    }

    @Test
    void emptyClipboardHasNoBounds() {
        Clipboard clip = new Clipboard(EditBuffer.empty(), Vec.ZERO);
        assertNull(clip.buffer().blockBounds());
        assertNull(clip.pasteBounds(Vec.ZERO));
        assertEquals(0, clip.pasteBuffer(Vec.ZERO, false).sectionCount());
    }

    @Test
    void pasteBoundsMatchThePastedBuffer() {
        EditBuffer buffer = EditBuffer.builder()
            .setBlockState(5, 70, 5, Block.STONE)
            .setBlockState(9, 72, 6, Block.DIRT)
            .setBlockState(7, 71, 13, Block.GLASS)
            .build();
        Clipboard clip = new Clipboard(buffer, new Vec(6, 70, 7));
        Vec anchor = new Vec(-40, 100, 17);
        List<Transform> transforms = List.of(
            Transform.identity(),
            Transform.rotateY(90),
            Transform.rotateY(180),
            Transform.rotateY(270),
            Transform.rotateX(90),
            Transform.rotateZ(270),
            Transform.mirror(Transform.Axis.X),
            Transform.mirror(Transform.Axis.Y),
            Transform.mirror(Transform.Axis.Z).compose(Transform.rotateX(90))
        );
        for (Transform t : transforms) {
            Clipboard transformed = clip.withTransform(t);
            assertEquals(
                transformed.pasteBuffer(anchor, false).blockBounds(),
                transformed.pasteBounds(anchor),
                "under " + t
            );
        }
    }

    @Test
    void offAxisTransformCannotPaste() {
        Clipboard clip = new Clipboard(
            EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE).build(),
            Vec.ZERO
        ).withTransform(Transform.rotateY(45));
        assertThrows(IllegalArgumentException.class, () -> clip.pasteBuffer(Vec.ZERO, false));
    }

    @Test
    void pastingAtTheOriginWithoutATransformReusesTheBuffer() {
        EditBuffer buffer = EditBuffer.builder().setBlockState(1, 2, 3, Block.STONE).build();
        Clipboard clip = new Clipboard(buffer, new Vec(1, 2, 3));
        assertSame(buffer, clip.pasteBuffer(new Vec(1, 2, 3), false));
        assertNotSame(buffer, clip.pasteBuffer(new Vec(1, 2, 3), true));
    }
}
