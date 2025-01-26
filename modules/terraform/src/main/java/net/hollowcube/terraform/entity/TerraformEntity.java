package net.hollowcube.terraform.entity;

import net.hollowcube.terraform.compat.axiom.util.NbtUtil;
import net.hollowcube.terraform.event.TerraformPreSpawnEntityEvent;
import net.hollowcube.terraform.event.TerraformSpawnEntityEvent;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * <p>Optional interface for entities to implement some terraform-specific behavior such as reading from NBT data.</p>
 *
 * <p>Not all of this functionality is necessarily Terraform specific, but needs to be accessed from Terraform.</p>
 */
public interface TerraformEntity {

    static @NotNull CompoundBinaryTag writeToTagWithPassengers(@NotNull TerraformEntity entity) {
        var compound = entity.writeToTag();
        if (!(entity instanceof Entity minestomEntity))
            return compound; // Sanity check

        // Include passengers list if there are any passengers
        var passengers = minestomEntity.getPassengers();
        if (!passengers.isEmpty()) {
            var passengerTagList = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
            for (var passenger : passengers) {
                if (passenger instanceof TerraformEntity passengerEntity) {
                    passengerTagList.add(passengerEntity.writeToTag());
                }
            }
            compound = compound.put("Passengers", passengerTagList.build());
        }

        return compound;
    }

    static @Nullable Entity spawnWithPassengers(@Nullable Player source, @NotNull Instance instance, @NotNull CompoundBinaryTag tag) {
        var entity = createEntityUnspawned(source, instance, tag);
        if (entity == null) return null;

        var passengers = new ArrayList<Entity>();
        for (var passengerTag : tag.getList("Passengers", BinaryTagTypes.COMPOUND)) {
            var passenger = createEntityUnspawned(source, instance, (CompoundBinaryTag) passengerTag);
            if (passenger == null) continue;
            passengers.add(passenger);
        }

        var spawnPosition = Objects.requireNonNullElse(NbtUtil.readSpawnPosition(tag), Pos.ZERO);
        var event = new TerraformSpawnEntityEvent(source, instance, entity, spawnPosition);
        EventDispatcher.call(event);
        if (event.isCancelled()) return null;

        var spawnFutures = new CompletableFuture[passengers.size() + 1];
        spawnFutures[0] = event.getEntity().setInstance(instance, event.getPosition());
        for (int i = 0; i < passengers.size(); i++)
            spawnFutures[i + 1] = passengers.get(i).setInstance(instance, event.getPosition());
        CompletableFuture.allOf(spawnFutures).thenRun(() -> passengers.forEach(event.getEntity()::addPassenger));
        return entity;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static @Nullable Entity createEntityUnspawned(@Nullable Player source, @NotNull Instance instance, @NotNull CompoundBinaryTag tag) {
        var entityTypeName = tag.getString("id");
        var entityType = EntityType.fromNamespaceId(entityTypeName);
        if (entityType == null) return null;

        var preEvent = new TerraformPreSpawnEntityEvent(source, instance);
        EventDispatcher.call(preEvent);
        if (preEvent.isCancelled()) return null;

        var uuid = tag.get("UUID") instanceof IntArrayBinaryTag uuidTag ? UUIDUtils.fromNbt(uuidTag) : UUID.randomUUID();
        var entity = preEvent.getConstructor().apply(entityType, uuid);
        if (entity instanceof TerraformEntity tfEntity) {
            final CompoundBinaryTag fnbt = tag;
            entity.editEntityMeta(EntityMeta.class, ignored -> tfEntity.readData(fnbt));
        }

        return entity;
    }

    void readData(@NotNull CompoundBinaryTag tag);

    void writeData(@NotNull CompoundBinaryTag.Builder tag);

    default @NotNull CompoundBinaryTag writeToTag() {
        var builder = CompoundBinaryTag.builder();
        writeData(builder);
        return builder.build();
    }
}
