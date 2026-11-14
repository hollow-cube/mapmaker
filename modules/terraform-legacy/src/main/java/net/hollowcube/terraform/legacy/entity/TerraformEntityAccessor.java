package net.hollowcube.terraform.legacy.entity;

import net.hollowcube.terraform.entity.EntityAccessor;
import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.util.TypedData;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entities through {@link TerraformEntity}, whose data (passengers included) is the entity's NBT. Other entities
 * are not editable.
 */
public final class TerraformEntityAccessor implements EntityAccessor {

    @Override
    public @Nullable EntityEdit capture(@NotNull Instance instance, @NotNull UUID id) {
        var entity = instance.getEntityByUuid(id);
        return entity instanceof TerraformEntity editable ? edit(entity, editable) : null;
    }

    @Override
    public void spawn(@NotNull Instance instance, @NotNull UUID id, @NotNull Pos pos, @NotNull TypedData data) {
        var tag = data.nbt().putString("id", data.type());
        TerraformEntity.spawnWithPassengers(null, instance, tag, id, pos);
    }

    /**
     * Passengers are left as they are.
     */
    @Override
    public void apply(@NotNull Instance instance, @NotNull UUID id, @NotNull Pos pos, @NotNull TypedData data) {
        var entity = instance.getEntityByUuid(id);
        if (!(entity instanceof TerraformEntity editable)) return;
        entity.editEntityMeta(EntityMeta.class, _ -> editable.readData(data.nbt()));
        if (!entity.getPosition().equals(pos)) {
            var _ = entity.teleport(pos);
        }
    }

    /**
     * Removes its passengers too, since the entity's data carries them.
     */
    @Override
    public void remove(@NotNull Instance instance, @NotNull UUID id) {
        var entity = instance.getEntityByUuid(id);
        if (!(entity instanceof TerraformEntity)) return;
        for (Entity passenger : List.copyOf(entity.getPassengers())) passenger.remove();
        entity.remove();
    }

    @Override
    public @NotNull List<EntityEdit> entitiesIn(@NotNull Instance instance, @NotNull Area.Cuboid box) {
        List<EntityEdit> found = new ArrayList<>();
        for (Entity entity : instance.getEntities()) {
            if (!(entity instanceof TerraformEntity editable) || entity.getVehicle() != null) continue;
            if (box.contains(entity.getPosition())) found.add(edit(entity, editable));
        }
        return found;
    }

    private static @NotNull EntityEdit edit(@NotNull Entity entity, @NotNull TerraformEntity editable) {
        var data = new TypedData(entity.getEntityType().name(), TerraformEntity.writeToTagWithPassengers(editable));
        return new EntityEdit(entity.getUuid(), entity.getPosition(), data);
    }
}
