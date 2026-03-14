package net.hollowcube.compat.axiom;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.events.*;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundAckWorldPropertyPacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundEntitiesResponsePacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundMarkerResponsePacket;
import net.hollowcube.compat.axiom.packets.serverbound.*;
import net.hollowcube.compat.axiom.properties.registry.PropertyRegistry;
import net.hollowcube.posthog.PostHog;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import net.minestom.server.network.packet.server.play.ChangeGameStatePacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@ApiStatus.Internal
final class AxiomPacketHandler {

    private static final long MAX_ENTITY_PACKET_SIZE = 0x100000L;

    static <T extends ServerboundModPacket<T>> BiConsumer<Player, T> handle(BiConsumer<Player, T> handler) {
        return (player, packet) -> {
            if (AxiomPlayer.isEnabled(player)) handler.accept(player, packet);
        };
    }

    static <T extends ServerboundModPacket<T>> BiConsumer<Player, T> disabled(@Nullable String message) {
        return (player, _) -> {
            if (AxiomPlayer.isEnabled(player) && message != null) player.sendMessage(message);
        };
    }

    static void onHello(Player player, AxiomServerboundHelloPacket packet) {
        if (packet.apiVersion() < AxiomAPI.MIN_API_VERSION) {
            player.sendMessage("Incompatible Axiom API version. Please update your mod.");
        } else if (packet.apiVersion() > AxiomAPI.MAX_API_VERSION) {
            player.sendMessage("Axiom API version is too new. Please be patient while we update the server. For now, you can use an older version of the mod.");
        } else {
            AxiomPlayer.setVersion(player, packet.apiVersion());
        }

        AxiomPlayer.handlePendingEnable(player);
    }

    // Player Operations

    static void onSetFlySpeed(Player player, AxiomServerboundSetFlySpeedPacket packet) {
        player.setFlyingSpeed(packet.speed());
    }

    static void onTeleport(Player player, AxiomServerboundTeleportPacket packet) {
        var playerDimension = player.getInstance().getDimensionName();
        if (!packet.dimension().equals(playerDimension)) {
            PostHog.capture(player.getUuid().toString(), "axiom_teleport_dimension_mismatch", Map.of(
                "player_dimension", playerDimension,
                "packet_dimension", packet.dimension()
            ));
            return;
        }

        player.teleport(packet.position());
    }

    static void onSetGameMode(Player player, AxiomServerboundSetGameModePacket packet) {
        if (!packet.gameMode().allowFlying()) {
            player.sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.CHANGE_GAMEMODE, player.getGameMode().ordinal()));
            return;
        }
        player.setGameMode(packet.gameMode());
    }

    static void onSetWorldProperty(Player player, AxiomServerboundSetWorldPropertyPacket packet) {
        new AxiomClientboundAckWorldPropertyPacket(packet.sequence()).send(player);
        try {
            var property = PropertyRegistry.getProperty(packet.id());
            if (property == null) return;
            property.update(player, packet.value());
        } catch (Exception e) {
            PostHog.captureException(e, player.getUuid().toString());
        }
    }

    // Data Operations

    static void onMarkerDataRequest(Player player, AxiomServerboundMarkerRequestPacket packet) {
        var event = switch (packet.reason()) {
            case COPYING -> new AxiomMarkerDataRequestEvent.Copying(player, packet.id());
            case RIGHT_CLICK -> new AxiomMarkerDataRequestEvent.RightClick(player, packet.id());
            default -> new AxiomMarkerDataRequestEvent(player, packet.id());
        };
        EventDispatcher.call(event);
        if (event.getData() == null || event.isCancelled()) return;

        new AxiomClientboundMarkerResponsePacket(packet.id(), event.getData()).send(player);
    }

    static void onEntityDataRequest(Player player, AxiomServerboundEntityRequestPacket packet) {
        var event = new AxiomEntitiesDataRequestEvent(player, packet.ids());
        EventDispatcher.call(event);

        long size = 0;
        Map<UUID, CompoundBinaryTag> responses = new HashMap<>();

        for (UUID id : packet.ids()) {
            var data = event.getData(id);
            if (data == null) continue;

            long dataSize = getSize(data);
            if (dataSize > MAX_ENTITY_PACKET_SIZE) {
                new AxiomClientboundEntitiesResponsePacket(
                    packet.sequence(), false, Map.of(id, data)
                ).send(player);
            } else {
                if (size + dataSize > MAX_ENTITY_PACKET_SIZE) {
                    new AxiomClientboundEntitiesResponsePacket(packet.sequence(), false, responses).send(player);
                    responses.clear();
                    size = 0;
                }

                responses.put(id, data);
                size += dataSize;
            }
        }

        new AxiomClientboundEntitiesResponsePacket(packet.sequence(), true, responses).send(player);
    }

    // Blocks/World Operations

    static void onSetBlock(Player player, AxiomServerboundSetBlockPacket packet) {
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
                } else if (heldItem.has(DataComponents.CUSTOM_MODEL_DATA)) {
                    // Items with custom model data need to trigger interact event
                    // We need to offset as if we are placing on the block next to it, which is not what axiom sends.
                    var relBlockPosition = new BlockVec(pos.relative(packet.face().getOppositeFace()));
                    var event = new PlayerBlockInteractEvent(player, packet.hand(), existingBlock, relBlockPosition, packet.cursor(), packet.face());
                    EventDispatcher.call(event);
                    if (event.isCancelled() || event.isBlockingItemUse()) continue;
                }

                instance.placeBlock(new BlockHandler.PlayerPlacement(
                    block, existingBlock, instance, pos, player, packet.hand(), packet.face(),
                    (float) packet.cursor().x(), (float) packet.cursor().y(), (float) packet.cursor().z()
                ), packet.updateNeighbors() != null && packet.updateNeighbors().contains(pos));
            }
        } finally {
            player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
        }
    }

    static void onSetBuffer(Player player, AxiomServerboundSetBufferPacket packet) {
        var instance = player.getInstance();
        if (!instance.getDimensionName().equals(packet.dimension())) return;

        EventDispatcher.call(new AxiomApplyBufferEvent(player, packet.id(), packet.buffer()));
    }

    // Entity Operations

    static void onRemoveEntities(Player player, AxiomServerboundRemoveEntitiesPacket packet) {
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

    static void onSpawnEntities(Player player, AxiomServerboundSpawnEntitiesPacket packet) {
        var instance = player.getInstance();
        for (var entry : packet.entries()) {
            if (instance.getEntityByUuid(entry.id()) != null) continue;

            try {
                EventDispatcher.call(new AxiomTrySpawnEntityEvent(
                    player, entry.id(), entry.copyFrom(), entry.pos(), entry.nbt()
                ));
            } catch (Exception e) {
                PostHog.captureException(e, player.getUuid().toString());
                player.sendMessage(Component.translatable("axiom.entity_spawn_failed"));
            }
        }
    }

    static void onModifyEntities(Player player, AxiomServerboundModifyEntitiesPacket packet) {
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

    static void onAnnotationUpdates(Player player, AxiomServerboundAnnotationUpdatePacket packet) {
        EventDispatcher.call(new AxiomAnnotationActionEvent(player, packet.actions()));
    }

    // Utils

    private static long getSize(@Nullable BinaryTag tag) {
        return 1 + switch (tag) {
            case null -> 0;
            case ByteBinaryTag ignored -> 1L;
            case ShortBinaryTag ignored -> 2L;
            case IntBinaryTag ignored -> 4L;
            case LongBinaryTag ignored -> 8L;
            case FloatBinaryTag ignored -> 4L;
            case DoubleBinaryTag ignored -> 8L;
            case ByteArrayBinaryTag array -> 4L + array.value().length;
            case IntArrayBinaryTag array -> 4L + 4L * array.value().length;
            case LongArrayBinaryTag array -> 4L + 8L * array.value().length;
            case StringBinaryTag string -> 2L + string.value().length();
            case ListBinaryTag list -> {
                long size = 5L;
                for (var element : list) {
                    size += getSize(element);
                }
                yield size;
            }

            case CompoundBinaryTag compound -> {
                long size = 1;
                for (var key : compound.keySet()) {
                    size += 1L;
                    size += 2L + key.length();
                    size += getSize(compound.get(key));
                }
                yield size;
            }

            default -> throw new IllegalStateException("Unexpected value: " + tag);
        };
    }
}
