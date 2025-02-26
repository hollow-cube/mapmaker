package net.hollowcube.compat.axiom;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.events.AxiomApplyBufferEvent;
import net.hollowcube.compat.axiom.events.AxiomMarkerDataRequestEvent;
import net.hollowcube.compat.axiom.events.AxiomTryModifyEntityEvent;
import net.hollowcube.compat.axiom.events.AxiomTrySpawnEntityEvent;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundAckWorldPropertyPacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundMarkerResponsePacket;
import net.hollowcube.compat.axiom.packets.serverbound.*;
import net.hollowcube.compat.axiom.properties.registry.PropertyRegistry;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import net.minestom.server.network.packet.server.play.ChangeGameStatePacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@ApiStatus.Internal
final class AxiomPacketHandler {

    static <T extends ServerboundModPacket<T>> BiConsumer<@NotNull Player, @NotNull T> handle(BiConsumer<@NotNull Player, @NotNull T> handler) {
        return (player, packet) -> {
            if (AxiomPlayer.isEnabled(player)) handler.accept(player, packet);
        };
    }

    static <T extends ServerboundModPacket<T>> BiConsumer<@NotNull Player, @NotNull T> disabled(String message) {
        return (player, packet) -> {
            if (AxiomPlayer.isEnabled(player)) player.sendMessage(message);
        };
    }

    static void onHello(@NotNull Player player, @NotNull AxiomServerboundHelloPacket packet) {
        if (packet.apiVersion() < AxiomAPI.API_VERSION) {
            player.sendMessage("Incompatible Axiom API version. Please update your client.");
        } else {
            AxiomPlayer.setVersion(player, packet.apiVersion());
        }
    }

    // Player Operations

    static void onSetFlySpeed(@NotNull Player player, @NotNull AxiomServerboundSetFlySpeedPacket packet) {
        player.setFlyingSpeed(packet.speed());
    }

    static void onTeleport(@NotNull Player player, @NotNull AxiomServerboundTeleportPacket packet) {
        var playerDimension = player.getInstance().getDimensionName();
        if (!packet.dimension().equals(playerDimension)) {
//            logger.warn("Received axiom teleport to different dimension ({} -> {}) from {}", playerDimension, packet.dimension(), player.getUuid());
            return;
        }

        player.teleport(packet.position());
    }

    static void onSetGameMode(@NotNull Player player, @NotNull AxiomServerboundSetGameModePacket packet) {
        if (!packet.gameMode().allowFlying()) {
            player.sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.CHANGE_GAMEMODE, player.getGameMode().ordinal()));
            return;
        }
        player.setGameMode(packet.gameMode());
    }

    static void onSetWorldProperty(@NotNull Player player, @NotNull AxiomServerboundSetWorldPropertyPacket packet) {
        new AxiomClientboundAckWorldPropertyPacket(packet.sequence()).send(player);
        try {
            var property = PropertyRegistry.getProperty(packet.id());
            if (property == null) return;
            property.update(player, packet.value());
        } catch (Exception e) {
            // TODO do some logging of bad world properties
        }
    }

    // Marker Operations

    static void onMarkerDataRequest(@NotNull Player player, @NotNull AxiomServerboundMarkerRequestPacket packet) {
        var event = new AxiomMarkerDataRequestEvent(player, packet.id());
        EventDispatcher.call(event);
        if (event.getData() == null || event.isCancelled()) return;

        new AxiomClientboundMarkerResponsePacket(packet.id(), event.getData()).send(player);
    }

    // Blocks/World Operations

    static void onSetBlock(@NotNull Player player, @NotNull AxiomServerboundSetBlockPacket packet) {
        try {
            var instance = player.getInstance();
            var heldItem = player.getItemInHand(packet.hand());

            for (var entry : packet.blocks().entrySet()) {
                var pos = entry.getKey();
                var block = entry.getValue();
                var existingBlock = instance.getBlock(pos);

                if (block.isAir()) {
                    // Air needs to trigger break event to handle cancellation (e.g. for worldedit wand)
                    var event = new PlayerBlockBreakEvent(player, existingBlock, block, new BlockVec(pos), packet.face());
                    EventDispatcher.call(event);
                    if (event.isCancelled()) continue;
                } else if (heldItem.has(ItemComponent.CUSTOM_MODEL_DATA)) {
                    // Items with custom model data need to trigger interact event
                    // We need to offset as if we are placing on the block next to it, which is not what axiom sends.
                    var relBlockPosition = new BlockVec(pos.relative(packet.face().getOppositeFace()));
                    var event = new PlayerBlockInteractEvent(player, packet.hand(), existingBlock, relBlockPosition, packet.cursor(), packet.face());
                    EventDispatcher.call(event);
                    if (event.isCancelled() || event.isBlockingItemUse()) continue;
                }

                instance.placeBlock(new BlockHandler.PlayerPlacement(
                        block, instance, pos, player, packet.hand(), packet.face(),
                        (float) packet.cursor().x(), (float) packet.cursor().y(), (float) packet.cursor().z()
                ), packet.updateNeighbors());
            }
        } finally {
            player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
        }
    }

    static void onSetBuffer(@NotNull Player player, @NotNull AxiomServerboundSetBufferPacket packet) {
        var instance = player.getInstance();
        if (!instance.getDimensionName().equals(packet.dimension())) return;

        EventDispatcher.call(new AxiomApplyBufferEvent(player, packet.id(), packet.buffer()));
    }

    // Entity Operations

    static void onRemoveEntities(@NotNull Player player, @NotNull AxiomServerboundRemoveEntitiesPacket packet) {
        var instance = player.getInstance();
        for (var uuid : packet.entities()) {
            var entity = instance.getEntityByUuid(uuid);
            if (entity != null && entity.getInstance().equals(instance)) {
                if (entity instanceof Player) continue;
                if (entity.getPassengers().stream().anyMatch(p -> p instanceof Player)) continue;

                entity.remove();
            }
        }
    }

    static void onSpawnEntities(@NotNull Player player, @NotNull AxiomServerboundSpawnEntitiesPacket packet) {
        var instance = player.getInstance();
        for (var entry : packet.entries()) {
            if (instance.getEntityByUuid(entry.id()) != null) continue;

            try {
                EventDispatcher.call(new AxiomTrySpawnEntityEvent(
                        player, entry.id(), entry.copyFrom(), entry.pos(), entry.nbt()
                ));
            } catch (Exception e) {
                // logger.warn("Failed to spawn axiom entity: {}", e.getMessage());
                player.sendMessage(Component.translatable("axiom.entity_spawn_failed"));
            }
        }
    }

    static void onModifyEntities(@NotNull Player player, @NotNull AxiomServerboundModifyEntitiesPacket packet) {
        var instance = player.getInstance();
        for (var entry : packet.entries()) {
            var entity = instance.getEntityByUuid(entry.id());
            if (entity == null || entity instanceof Player) continue;
            if (!entity.getInstance().equals(instance)) continue;

            var pos = entry.pos();
            if (pos != null) {
                var newX = entry.hasFlag(RelativeFlags.X) ? entity.getPosition().x() + pos.x() : pos.x();
                var newY = entry.hasFlag(RelativeFlags.Y) ? entity.getPosition().y() + pos.y() : pos.y();
                var newZ = entry.hasFlag(RelativeFlags.Z) ? entity.getPosition().z() + pos.z() : pos.z();
                var newYaw = entry.hasFlag(RelativeFlags.YAW) ? entity.getPosition().yaw() + pos.yaw() : pos.yaw();
                var newPitch = entry.hasFlag(RelativeFlags.PITCH) ? entity.getPosition().pitch() + pos.pitch() : pos.pitch();

                pos = new Pos(newX, newY, newZ, newYaw, newPitch);
            }

            EventDispatcher.call(new AxiomTryModifyEntityEvent(
                    player, entity, pos, entry.nbt(), entry.passengerChange(), entry.passengers()
            ));
        }
    }
}
