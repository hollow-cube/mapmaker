package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.TestInstances;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.entity.EntityAccessor;
import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.entity.TestEntityAccessor;
import net.hollowcube.terraform.util.TypedData;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class EntityApplyTest {
    private static final ApplyEngine ENGINE = ApplyEngine.builder()
        .entities(TestEntityAccessor.INSTANCE)
        .build();
    private static final String ZOMBIE = EntityType.ZOMBIE.name();
    private static final String COW = EntityType.COW.name();

    @Test
    void spawnsAndUndoRemoves(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var spawn = new EntityEdit(UUID.randomUUID(), new Pos(3, 70, 4, 90, 0), data(ZOMBIE, "a"));

        var result = ENGINE.apply(instance, edits(spawn));

        assertEquals(1, result.changed());
        assertEquals(spawn, TestEntityAccessor.INSTANCE.capture(instance, spawn.id()));
        assertEquals(
            List.of(EntityEdit.remove(spawn.id(), spawn.pos())),
            result.undo().entities(),
            "the undo removes it from where it was spawned"
        );
        ENGINE.apply(instance, result.undo());
        assertNull(instance.getEntityByUuid(spawn.id()));
    }

    @Test
    void updatesAndUndoRestores(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID id = spawn(instance, ZOMBIE, new Pos(1, 70, 1), "before");
        var before = TestEntityAccessor.INSTANCE.capture(instance, id);
        var after = new EntityEdit(id, new Pos(5, 71, 6), data(ZOMBIE, "after"));

        var result = ENGINE.apply(instance, edits(after));

        assertEquals(after, TestEntityAccessor.INSTANCE.capture(instance, id));
        ENGINE.apply(instance, result.undo());
        assertEquals(before, TestEntityAccessor.INSTANCE.capture(instance, id));
    }

    @Test
    void removesAndUndoRespawnsTheSameEntity(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID id = spawn(instance, ZOMBIE, new Pos(2, 70, 2), "kept");
        var before = TestEntityAccessor.INSTANCE.capture(instance, id);

        var result = ENGINE.apply(instance, edits(EntityEdit.remove(id, new Pos(2, 70, 2))));

        assertNull(instance.getEntityByUuid(id));
        ENGINE.apply(instance, result.undo());
        assertEquals(before, TestEntityAccessor.INSTANCE.capture(instance, id));
    }

    @Test
    void aTypeChangeRespawnsUnderTheSameId(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID id = spawn(instance, ZOMBIE, new Pos(2, 70, 2), "zombie");
        var cow = new EntityEdit(id, new Pos(2, 70, 2), data(COW, "cow"));

        var result = ENGINE.apply(instance, edits(cow));

        assertEquals(cow, TestEntityAccessor.INSTANCE.capture(instance, id));
        ENGINE.apply(instance, result.undo());
        assertEquals(ZOMBIE, captured(instance, id).data().type());
    }

    @Test
    void undoUnwindsSeveralEditsOfOneEntity(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID id = UUID.randomUUID();
        var buffer = edits(
            new EntityEdit(id, new Pos(1, 70, 1), data(ZOMBIE, "one")),
            new EntityEdit(id, new Pos(2, 70, 2), data(ZOMBIE, "two"))
        );

        var result = ENGINE.apply(instance, buffer);
        assertEquals(new Pos(2, 70, 2), captured(instance, id).pos());

        ENGINE.apply(instance, result.undo());
        assertNull(instance.getEntityByUuid(id), "undoes the update, then the spawn");
    }

    @Test
    void anEditMatchingTheWorldChangesNothing(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID id = spawn(instance, ZOMBIE, new Pos(1, 70, 1), "same");

        var result = ENGINE.apply(
            instance,
            edits(captured(instance, id), EntityEdit.remove(UUID.randomUUID(), Pos.ZERO))
        );

        assertEquals(0, result.changed());
        assertTrue(result.undo().entities().isEmpty());
    }

    @Test
    void aThrowingAccessorSkipsOnlyThatEdit(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID broken = UUID.randomUUID(), fine = UUID.randomUUID();
        var engine = ApplyEngine.builder().entities(new ThrowingFor(broken)).build();

        var result = engine.apply(
            instance,
            edits(
                new EntityEdit(broken, new Pos(1, 70, 1), data(ZOMBIE, "x")),
                new EntityEdit(fine, new Pos(2, 70, 2), data(ZOMBIE, "y"))
            )
        );

        assertEquals(1, result.changed());
        assertNull(instance.getEntityByUuid(broken));
        assertNotNull(instance.getEntityByUuid(fine));
    }

    @Test
    void submittedAppliesWriteEntitiesAfterBlocks(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID id = UUID.randomUUID();
        var buffer = EditBuffer.builder()
            .setBlockState(1, 70, 1, Block.STONE)
            .addEntityEdit(new EntityEdit(id, new Pos(1, 71, 1), data(ZOMBIE, "q")))
            .build();

        var result = ENGINE.submit(instance, buffer);
        env.tickWhile(() -> !result.isDone(), Duration.ofSeconds(5));

        assertEquals(2, result.join().changed());
        assertEquals(Block.STONE, instance.getBlock(1, 70, 1));
        assertNotNull(instance.getEntityByUuid(id));
    }

    @Test
    void entityEditsNeedAnAccessor(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        var buffer = EditBuffer.builder()
            .setBlockState(1, 70, 1, Block.STONE)
            .addEntityEdit(new EntityEdit(UUID.randomUUID(), new Pos(1, 71, 1), data(ZOMBIE, "x")))
            .build();

        assertThrows(IllegalStateException.class, () -> ApplyEngine.DEFAULT.apply(instance, buffer));
        assertThrows(CompletionException.class, () -> ApplyEngine.DEFAULT.submit(instance, buffer).join());
        assertEquals(Block.AIR, instance.getBlock(1, 70, 1), "refused before writing");
    }

    @Test
    void entitiesInFindsRootEntitiesInTheBox(Env env) {
        Instance instance = TestInstances.flatChunk(env);
        UUID inside = spawn(instance, ZOMBIE, new Pos(2.5, 70, 2.5), "in");
        spawn(instance, ZOMBIE, new Pos(14, 70, 14), "out");
        UUID rider = spawn(instance, ZOMBIE, new Pos(3, 70, 3), "rider");
        instance.getEntityByUuid(inside).addPassenger(instance.getEntityByUuid(rider));

        var found = TestEntityAccessor.INSTANCE.entitiesIn(
            instance,
            Area.cuboid(new BlockVec(0, 60, 0), new BlockVec(8, 80, 8))
        );

        assertEquals(List.of(inside), found.stream().map(EntityEdit::id).toList());
    }

    private static EditBuffer edits(EntityEdit... edits) {
        var builder = EditBuffer.builder();
        for (EntityEdit edit : edits) builder.addEntityEdit(edit);
        return builder.build();
    }

    private static UUID spawn(Instance instance, String type, Pos pos, String value) {
        var entity = new Entity(EntityType.fromKey(type), UUID.randomUUID());
        entity.tagHandler().updateContent(tag(value));
        entity.setInstance(instance, pos).join();
        return entity.getUuid();
    }

    private static EntityEdit captured(Instance instance, UUID id) {
        var edit = TestEntityAccessor.INSTANCE.capture(instance, id);
        assertNotNull(edit);
        return edit;
    }

    private static TypedData data(String type, String value) {
        return new TypedData(type, tag(value));
    }

    private static CompoundBinaryTag tag(String value) {
        return CompoundBinaryTag.builder().putString("value", value).build();
    }

    record ThrowingFor(UUID broken) implements EntityAccessor {
        @Override
        public @Nullable EntityEdit capture(Instance instance, UUID id) {
            if (id.equals(broken)) throw new IllegalStateException("broken accessor");
            return TestEntityAccessor.INSTANCE.capture(instance, id);
        }

        @Override
        public void spawn(Instance instance, UUID id, Pos pos, TypedData data) {
            TestEntityAccessor.INSTANCE.spawn(instance, id, pos, data);
        }

        @Override
        public void apply(Instance instance, UUID id, Pos pos, TypedData data) {
            TestEntityAccessor.INSTANCE.apply(instance, id, pos, data);
        }

        @Override
        public void remove(Instance instance, UUID id) {
            TestEntityAccessor.INSTANCE.remove(instance, id);
        }

        @Override
        public List<EntityEdit> entitiesIn(Instance instance, Area.Cuboid box) {
            return TestEntityAccessor.INSTANCE.entitiesIn(instance, box);
        }
    }
}
