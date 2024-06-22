package net.hollowcube.terraform.compat.axiom.listener;

import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomRequestMarkerDataEvent;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomUpdateMarkerDataEvent;
import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.hollowcube.terraform.compat.axiom.packet.server.*;
import net.hollowcube.terraform.compat.axiom.world.property.WorldPropertiesRegistry;
import net.hollowcube.terraform.entity.TerraformEntity;
import net.hollowcube.terraform.event.TerraformModifyEntityEvent;
import net.hollowcube.terraform.event.TerraformMoveEntityEvent;
import net.hollowcube.terraform.event.TerraformPreSpawnEntityEvent;
import net.hollowcube.terraform.event.TerraformSpawnEntityEvent;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public final class AxiomPacketListener {
    private static final Logger logger = LoggerFactory.getLogger(AxiomPacketListener.class);

    public void handleHelloMessage(@NotNull Player player, @NotNull AxiomClientHelloPacket packet) {
        var clientInfo = new Axiom.ClientInfo(packet.apiVersion(), packet.extraData());
        logger.info("Axiom is present for {} (API {})", player.getUsername(), clientInfo.apiVersion());
        if (clientInfo.apiVersion() < Axiom.MIN_API_VERSION) {
            player.sendMessage("Your version of Axiom is too old, please update to the latest version.");
        } else if (clientInfo.apiVersion() > Axiom.MAX_API_VERSION) {
            player.sendMessage("Your version of Axiom is not yet supported. Please be patient while we update.");
        } else {
            player.setTag(Axiom.CLIENT_INFO_TAG, clientInfo);

            // It may have been enabled before the hello message was received, so check that.
            if (Axiom.isEnabled(player)) {
                Axiom.enable(player); // This time the hello message will be sent.
            } else {
                var disablePacket = new AxiomEnablePacket(false);
                player.sendPacket(disablePacket.toPacket(player));
            }
        }
    }

    public void handleSetGamemode(@NotNull Player player, @NotNull AxiomClientSetGameModePacket packet) {
        if (!Axiom.isEnabled(player)) return;
        try {
            player.setGameMode(GameMode.fromId(packet.gameModeId()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid gamemode received from {} ({})", player.getUuid(), packet.gameModeId());
        }
    }

    public void handleSetFlySpeed(@NotNull Player player, @NotNull AxiomClientSetFlySpeedPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        player.setFlyingSpeed(packet.flySpeed());
    }

    public void handleSetHotbarSlot(@NotNull Player player, @NotNull AxiomClientSetHotbarSlotPacket packet) {

    }

    public void handleSwitchActiveHotbar(@NotNull Player player, @NotNull AxiomClientSwitchActiveHotbarPacket packet) {

    }

    public void handleTeleport(@NotNull Player player, @NotNull AxiomClientTeleportPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var playerDimensionName = player.getInstance().getDimensionName();
        if (!packet.dimensionName().equals(playerDimensionName)) {
            logger.warn("Received axiom teleport to different dimension ({} -> {}) from {}",
                    playerDimensionName, packet.dimensionName(), player.getUuid());
            return;
        }

        player.teleport(packet.position());
    }

    public void handleSetEditorViews(@NotNull Player player, @NotNull AxiomClientSetEditorViewsPacket packet) {

    }

    public void handleRequestChunkData(@NotNull Player player, @NotNull AxiomClientChunkDataRequestPacket packet) {
        //todo need to properly handle this in the future, for now just send back an empty response always
        var response = new AxiomChunkDataResponsePacket(packet.correlationId());
        player.sendPacket(response.toPacket(player));
    }

    public void handleSetBlock(@NotNull Player player, @NotNull AxiomClientSetBlockPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        try {
            //todo use the other stuff here to do whatever fancy things.
            var instance = player.getInstance();
            var blockFace = packet.blockHit().blockFace();

            var itemInHand = player.getItemInHand(packet.hand());
            for (var entry : packet.blocks().entrySet()) {
                var blockPosition = entry.getKey();
                var block = entry.getValue();
                var existingBlock = instance.getBlock(blockPosition);

                CancellableEvent event = null;
                if (block.isAir()) {
                    // Air needs to trigger break event to handle cancellation (e.g. for worldedit wand)
                    event = new PlayerBlockBreakEvent(player, existingBlock, block, new BlockVec(blockPosition), blockFace);
                } else if (itemInHand.has(ItemComponent.CUSTOM_MODEL_DATA)) {
                    // Items with custom model data need to trigger interact event
                    // We need to offset as if we are placing on the block next to it, which is not what axiom sends.
                    var relBlockPosition = blockPosition.relative(blockFace.getOppositeFace());
                    event = new PlayerBlockInteractEvent(player, packet.hand(), existingBlock, new BlockVec(relBlockPosition),
                            packet.blockHit().cursorPosition(), blockFace);
                }

                // Update the block if the event isnt cancelled
                if (event != null) EventDispatcher.call(event);
                if (event == null || !event.isCancelled()) {
                    if (!(event instanceof PlayerBlockInteractEvent interact) || !interact.isBlockingItemUse())
                        instance.setBlock(blockPosition, block, packet.updateNeighbors());
                }
            }
        } finally {
            player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
        }
    }

    public void handleSetBuffer(@NotNull Player player, @NotNull AxiomClientSetBufferPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.warn("Received axiom:set_buffer from {}", player.getUuid());

        var playerDimensionName = player.getInstance().getDimensionName();
        if (!packet.dimensionName().equals(playerDimensionName)) {
            logger.warn("Received axiom set buffer for a different dimension ({} -> {}) from {}",
                    playerDimensionName, packet.dimensionName(), player.getUuid());
            return;
        }

        var session = LocalSession.forPlayer(player);
        session.buildTask("axiom-" + packet.correlationId())
                .metadata() //todo
                .buffer(packet.buffer())
                // Set the task as ephemeral, so it is not appended to the history stack
                // Axiom will manage undoing these changes itself.
                .ephemeral()
                .submit();
    }

    public void handleSetWorldProperty(@NotNull Player player, @NotNull AxiomClientSetWorldPropertyPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.warn("Received axiom:set_world_property from {}", player.getUuid());

        var registry = WorldPropertiesRegistry.get(player.getInstance());
        var handled = registry.handlePropertyChange(player, packet);
        if (!handled) return;

        var response = new AxiomAckWorldPropertyPacket(packet.sequenceId());
        player.sendPacket(response.toPacket(player));
    }

    public void handleSetTime(@NotNull Player player, @NotNull AxiomClientSetTimePacket packet) {

    }

    public void handleSpawnEntities(@NotNull Player player, @NotNull AxiomClientSpawnEntitiesPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var instance = player.getInstance();
        for (var entry : packet.entries()) {
            try {
                CompoundBinaryTag nbt = entry.nbt();
                if (entry.copyFrom() != null) {
                    final Entity copyEntity = instance.getEntityByUuid(entry.copyFrom());
                    if (!(copyEntity instanceof TerraformEntity entity)) return;
                    nbt = entity.writeToTag();
                }

                var entityTypeName = nbt.getString("id");
                var entityType = EntityType.fromNamespaceId(entityTypeName);
                if (entityType == null) throw new NullPointerException("unknown entity type: " + entityTypeName);

                var preEvent = new TerraformPreSpawnEntityEvent(player, instance);
                EventDispatcher.call(preEvent);
                if (preEvent.isCancelled()) continue;

                var entity = preEvent.getConstructor().apply(entityType, entry.uuid());
                if (entity instanceof TerraformEntity tfEntity) {
                    final CompoundBinaryTag fnbt = nbt;
                    entity.editEntityMeta(EntityMeta.class, _ -> tfEntity.readData(fnbt));
                }

                var event = new TerraformSpawnEntityEvent(player, instance, entity, entry.pos());
                EventDispatcher.callCancellable(event, () -> event.getEntity().setInstance(instance, event.getPosition()));

                //todo handle passengers
            } catch (Exception e) {
                logger.warn("Failed to spawn axiom entity: {}", e.getMessage());
                player.sendMessage(Component.translatable("axiom.entity_spawn_failed"));
            }
        }
    }

    public void handleModifyEntities(@NotNull Player player, @NotNull AxiomClientModifyEntitiesPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        //todo this does not handle fake entities, it probably should. That would be particularly useful for markers.
        var instance = player.getInstance();
        for (var entry : packet.entries()) {
            var entity = instance.getEntityByUuid(entry.uuid());
            if (entity == null) {
                logger.warn("Axiom tried to modify an unknown entity: {} player={}", entry.uuid(), player.getUuid());
                continue;
            }
            if (!instance.equals(entity.getInstance()) || entity instanceof Player) continue;

            var entryPos = entry.pos();
            if (entryPos != null) {
                var newX = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_X) != 0 ? entity.getPosition().x() + entryPos.x() : entryPos.x();
                var newY = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_Y) != 0 ? entity.getPosition().y() + entryPos.y() : entryPos.y();
                var newZ = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_Z) != 0 ? entity.getPosition().z() + entryPos.z() : entryPos.z();
                var newYaw = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_YAW) != 0 ? entity.getPosition().yaw() + entryPos.yaw() : entryPos.yaw();
                var newPitch = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_PITCH) != 0 ? entity.getPosition().pitch() + entryPos.pitch() : entryPos.pitch();

                var event = new TerraformMoveEntityEvent(player, entity, new Pos(newX, newY, newZ, newYaw, newPitch));
                EventDispatcher.callCancellable(event, () -> entity.teleport(event.getNewPosition()));
            }

            if (entity.getEntityType().equals(EntityType.MARKER)) {
                // Markers have special NBT handling for now.
                //todo i think i can get rid of this special case now that we have TerraformEntity#readData
                var event = new TerraformAxiomUpdateMarkerDataEvent(player, entry.uuid(), entry.nbt());
                EventDispatcher.call(event);
            } else {
                if (entity instanceof TerraformEntity tfEntity)
                    entity.editEntityMeta(EntityMeta.class, $ -> tfEntity.readData(entry.nbt()));
                EventDispatcher.call(new TerraformModifyEntityEvent(entity));
            }

            //todo passengers
        }
    }

    public void handleDeleteEntities(@NotNull Player player, @NotNull AxiomClientDeleteEntitiesPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var instance = player.getInstance();
        for (var uuid : packet.uuids()) {
            var entity = instance.getEntityByUuid(uuid);
            if (entity == null) {
                logger.warn("Axiom tried to delete an unknown entity: {} player={}", uuid, player.getUuid());
                continue;
            }

            if (!instance.equals(entity.getInstance()) || entity instanceof Player ||
                    entity.getPassengers().stream().anyMatch(e -> e instanceof Player)) continue;

            entity.remove();
        }
    }

    public void handleRequestMarkerData(@NotNull Player player, @NotNull AxiomClientMarkerNbtRequestPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var event = new TerraformAxiomRequestMarkerDataEvent(player, packet.uuid());
        EventDispatcher.callCancellable(event, () -> {
            if (event.getData() == null) return;

            var responsePacket = new AxiomMarkerNbtResponsePacket(packet.uuid(), event.getData());
            player.sendPacket(responsePacket.toPacket(player));
        });
    }

    private @Nullable AxiomMarkerDataPacket.Entry createAddMarkerEntry(@NotNull Entity entity) {
        if (!entity.getEntityType().equals(EntityType.MARKER)) return null;
        return new AxiomMarkerDataPacket.Entry(entity.getUuid(), entity.getPosition(), null, null, null, 0, 0, 0);
    }

}
