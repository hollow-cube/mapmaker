package net.hollowcube.terraform.clipboard;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.entity.EntityAccessor;
import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.entity.TestEntityAccessor;
import net.hollowcube.terraform.util.Transform;
import net.hollowcube.terraform.util.TypedData;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClipboardEntityTest {
    private static final String ZOMBIE = "minecraft:zombie";
    private static final UUID ID = UUID.randomUUID();

    @Test
    void pasteBufferLeavesEntitiesOutWithoutAnAccessor() {
        var clip = new Clipboard(withZombie(new Pos(1.5, 0, 1.5)), new BlockVec(0, 0, 0));

        assertTrue(clip.pasteBuffer(new BlockVec(10, 0, 0), false, null).entities().isEmpty());
    }

    @Test
    void pasteEntitiesMovesLikeTheBlocksWithFreshIds() {
        var clip = new Clipboard(withZombie(new Pos(1.5, 0, 1.5, 0, 0)), new BlockVec(0, 0, 0));

        var pasted = clip.pasteBuffer(new BlockVec(10, 5, 10), false, TestEntityAccessor.INSTANCE).entities();

        assertEquals(1, pasted.size());
        assertNotEquals(ID, pasted.getFirst().id());
        assertEquals(new Pos(11.5, 5, 11.5, 0, 0), pasted.getFirst().pos());
    }

    @Test
    void pasteEntitiesRotatesPositionAndFacing() {
        var clip = new Clipboard(withZombie(new Pos(2.5, 0, 0.5, 0, 0)), new BlockVec(0, 0, 0))
            .withTransform(Transform.rotateY(90));

        var pos = clip.pasteBuffer(BlockVec.ZERO, false, TestEntityAccessor.INSTANCE).entities()
            .getFirst()
            .pos();

        // East of the origin turns to south, and facing south (yaw 0) turns to facing west (yaw 90)
        assertEquals(0.5, pos.x(), 1e-9);
        assertEquals(2.5, pos.z(), 1e-9);
        assertEquals(90, pos.yaw(), 1e-4);
    }

    @Test
    void pasteEntitiesGoThroughTheHostCopy() {
        var clip = new Clipboard(withZombie(new Pos(0.5, 0, 0.5)), new BlockVec(0, 0, 0));
        var renaming = new Renaming();

        var data = clip.pasteBuffer(BlockVec.ZERO, false, renaming).entities().getFirst().data();

        assertEquals("copied", data.nbt().getString("name"));
    }

    @Test
    void savingATransformedClipboardMovesItsEntitiesButKeepsTheirIds() {
        var clip = new Clipboard(withZombie(new Pos(2.5, 0, 0.5)), new BlockVec(0, 0, 0))
            .withTransform(Transform.rotateY(90));

        var saved = clip.toSchematic().buffer().entities().getFirst();

        assertEquals(ID, saved.id());
        assertEquals(0.5, saved.pos().x(), 1e-9);
        assertEquals(2.5, saved.pos().z(), 1e-9);
    }

    private static EditBuffer withZombie(Pos pos) {
        return EditBuffer.builder()
            .setBlockState(0, 0, 0, 1)
            .addEntityEdit(
                new EntityEdit(ID, pos, new TypedData(ZOMBIE, CompoundBinaryTag.empty()))
            )
            .build();
    }

    private static final class Renaming implements EntityAccessor {
        @Override
        public @Nullable EntityEdit capture(Instance instance, UUID id) {
            return null;
        }

        @Override
        public void spawn(Instance instance, UUID id, Pos pos, TypedData data) {}

        @Override
        public void apply(Instance instance, UUID id, Pos pos, TypedData data) {}

        @Override
        public void remove(Instance instance, UUID id) {}

        @Override
        public List<EntityEdit> entitiesIn(Instance instance, Area.Cuboid box) {
            return List.of();
        }

        @Override
        public TypedData copy(TypedData data, Transform transform) {
            return new TypedData(data.type(), data.nbt().putString("name", "copied"));
        }
    }
}
