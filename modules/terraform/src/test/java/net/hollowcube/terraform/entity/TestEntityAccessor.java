package net.hollowcube.terraform.entity;

import net.hollowcube.terraform.util.TypedData;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// Plain Minestom [Entity]s, with `nbt` as their tag data (replaced wholesale on update).
public final class TestEntityAccessor implements EntityAccessor {
    public static final TestEntityAccessor INSTANCE = new TestEntityAccessor();

    @Override
    public @Nullable EntityEdit capture(Instance instance, UUID id) {
        Entity entity = editable(instance, id);
        return entity != null ? edit(entity) : null;
    }

    @Override
    public void spawn(Instance instance, UUID id, Pos pos, TypedData data) {
        EntityType type = EntityType.fromKey(data.type());
        if (type == null || instance.getEntityByUuid(id) != null) return;
        var entity = new Entity(type, id);
        entity.tagHandler().updateContent(data.nbt());
        var _ = entity.setInstance(instance, pos);
    }

    @Override
    public void apply(Instance instance, UUID id, Pos pos, TypedData data) {
        Entity entity = editable(instance, id);
        if (entity == null) return;
        entity.tagHandler().updateContent(data.nbt());
        if (!entity.getPosition().equals(pos)) {
            var _ = entity.teleport(pos);
        }
    }

    @Override
    public void remove(Instance instance, UUID id) {
        Entity entity = editable(instance, id);
        if (entity != null) entity.remove();
    }

    @Override
    public List<EntityEdit> entitiesIn(Instance instance, Area.Cuboid box) {
        List<EntityEdit> found = new ArrayList<>();
        for (Entity entity : instance.getEntities()) {
            if (entity instanceof Player || entity.getVehicle() != null) continue;
            if (box.contains(entity.getPosition())) found.add(edit(entity));
        }
        return found;
    }

    private static EntityEdit edit(Entity entity) {
        return new EntityEdit(
            entity.getUuid(),
            entity.getPosition(),
            new TypedData(entity.getEntityType().name(), entity.tagHandler().asCompound())
        );
    }

    private static @Nullable Entity editable(Instance instance, UUID id) {
        Entity entity = instance.getEntityByUuid(id);
        return entity instanceof Player ? null : entity;
    }

    private TestEntityAccessor() {}
}
