package net.hollowcube.terraform.compat.axiom;

import net.hollowcube.compat.axiom.data.annotations.actions.AnnotationAction;
import net.hollowcube.compat.axiom.data.buffers.AxiomBlockBuffer;
import net.hollowcube.compat.axiom.events.*;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundAnnotationUpdatePacket;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomUpdateCustomEntityDataEvent;
import net.hollowcube.terraform.compat.axiom.util.AxiomAnnotationStorage;
import net.hollowcube.terraform.compat.axiom.util.AxiomTerraformBuffer;
import net.hollowcube.terraform.compat.axiom.util.NbtUtil;
import net.hollowcube.terraform.entity.TerraformEntity;
import net.hollowcube.terraform.event.TerraformModifyEntityEvent;
import net.hollowcube.terraform.event.TerraformMoveEntityEvent;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.Messages;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AxiomModule implements TerraformModule {

    private final EventNode<Event> axiomEvents = EventNode.all("tf/compat/axiom")
            .addListener(AxiomTrySpawnEntityEvent.class, this::handleEntitySpawn)
            .addListener(AxiomTryModifyEntityEvent.class, this::handleEntityModification)
            .addListener(AxiomApplyBufferEvent.class, this::handleBufferApplication)
            .addListener(AxiomAnnotationActionEvent.class, this::handleAnnotationActions)
            .addListener(AxiomEnabledEvent.class, this::handleAxiomEnabled)
            .addListener(AxiomEntitiesDataRequestEvent.class, this::handleEntityDataRequest);

    @Override
    public @NotNull Set<EventNode<InstanceEvent>> eventNodes() {
        //todo this is NOT good, need some way to provide global event handlers
        MinecraftServer.getGlobalEventHandler().removeChild(axiomEvents);
        MinecraftServer.getGlobalEventHandler().addChild(axiomEvents);
        return Set.of();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void handleEntitySpawn(@NotNull AxiomTrySpawnEntityEvent event) {
        if (event.isHandled()) return;

        CompoundBinaryTag nbt = event.nbt() == null ? CompoundBinaryTag.empty() : event.nbt();
        if (event.copyFrom() != null) {
            if (!(event.copyFrom() instanceof TerraformEntity entity)) return;
            var copyEntityTag = TerraformEntity.writeToTagWithPassengers(entity);
            nbt = NbtUtil.mergeNbtCompounds(nbt, copyEntityTag);
        }

        // We are going to now spawn an entity based on the given inputs, but we need to make a few edits:
        // - Root should have the ID given by Axiom
        // - Root should have the position given by Axiom
        // - Passengers should have random IDs
        nbt = nbt.put(Map.of(
                "UUID", UUIDUtils.toNbt(event.uuid()),
                "Pos", NbtUtil.toPosTag(event.pos()),
                "Rotation", NbtUtil.toRotationTag(event.pos()),
                "Passengers", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, nbt
                        .getList("Passengers", BinaryTagTypes.COMPOUND)
                        .stream().map(passengerTag -> (BinaryTag) ((CompoundBinaryTag) passengerTag).remove("UUID"))
                        .toList())
        ));

        TerraformEntity.spawnWithPassengers(event.player(), event.getInstance(), nbt);

        event.setHandled(true);
    }

    private void handleEntityModification(@NotNull AxiomTryModifyEntityEvent event) {
        if (event.isHandled()) return;

        var instance = event.getInstance();
        var entity = event.entity();
        var player = event.player();

        if (event.pos() != null) {
            var moveEvent = new TerraformMoveEntityEvent(player, entity, event.pos());
            EventDispatcher.callCancellable(moveEvent, () -> entity.teleport(moveEvent.getNewPosition()));
        }

        if (event.nbt() != null && event.nbt().size() > 0) {
            if (entity instanceof TerraformAxiomUpdateCustomEntityDataEvent.Receiver) {
                EventDispatcher.call(new TerraformAxiomUpdateCustomEntityDataEvent(event.player(), entity.getUuid(), event.nbt()));
            } else if (entity instanceof TerraformEntity tfEntity) {
                entity.editEntityMeta(EntityMeta.class, ignored -> tfEntity.readData(event.nbt()));
            }
        }

        switch (event.change()) {
            case ADD -> {
                for (var id : event.passengers()) {
                    var passenger = instance.getEntityByUuid(id);
                    var isInvalidPassenger = passenger == null ||
                            passenger.getEntityId() == entity.getEntityId() ||
                            passenger.getVehicle() != null ||
                            passenger instanceof Player ||
                            passenger.getPassengers().stream().anyMatch(otherPassenger ->
                                    otherPassenger instanceof Player || otherPassenger.getEntityId() == entity.getEntityId());

                    if (isInvalidPassenger) continue;
                    entity.addPassenger(passenger);
                }
            }
            case REMOVE -> {
                for (var id : event.passengers()) {
                    var passenger = instance.getEntityByUuid(id);
                    var isInvalidPassenger = passenger == null ||
                            passenger.getEntityId() == entity.getEntityId() ||
                            passenger instanceof Player ||
                            passenger.getPassengers().stream().anyMatch(Player.class::isInstance) ||
                            passenger.getVehicle() != entity;
                    if (isInvalidPassenger) continue;
                    entity.removePassenger(entity);
                }
            }
            case CLEAR -> Set.copyOf(entity.getPassengers()).forEach(entity::removePassenger);
        }

        event.setHandled(true);

        EventDispatcher.call(new TerraformModifyEntityEvent(entity));
    }

    private void handleEntityDataRequest(@NotNull AxiomEntitiesDataRequestEvent event) {
        for (Entity entity : event.entities()) {
            if (entity instanceof TerraformEntity tfEntity) {
                var builder = CompoundBinaryTag.builder();
                tfEntity.writeData(builder);
                event.setData(entity.getUuid(), builder.build());
            }
        }
    }

    private void handleBufferApplication(@NotNull AxiomApplyBufferEvent event) {
        if (event.isHandled()) return;
        if (!(event.buffer() instanceof AxiomBlockBuffer buffer)) return;

        var session = LocalSession.forPlayer(event.player());
        var task = session.buildTask("axiom-" + event.id())
                .metadata()
                .buffer(AxiomTerraformBuffer.of(buffer))
                .ephemeral()
                          .submitIfCapacity();
        if (task == null) {
            event.getPlayer().sendMessage(Messages.GENERIC_QUEUE_FULL);
        }
        event.setHandled(true);
    }

    private void handleAnnotationActions(@NotNull AxiomAnnotationActionEvent event) {
        if (event.isHandled()) return;
        var storage = AxiomAnnotationStorage.get(event.player());
        if (storage != null) {
            List<AnnotationAction> actions = new ArrayList<>();
            for (AnnotationAction action : event.actions()) {
                var result = storage.apply(event.player(), action);
                if (result != null) actions.add(result);
            }
            new AxiomClientboundAnnotationUpdatePacket(actions).sendToInstance(event.getInstance());
        }

        event.setHandled(true);
    }

    private void handleAxiomEnabled(@NotNull AxiomEnabledEvent event) {
        if (!event.isEnabled()) return;

        var storage = AxiomAnnotationStorage.get(event.getPlayer());
        if (storage != null) storage.sendAllTo(event.getPlayer());
    }
}
