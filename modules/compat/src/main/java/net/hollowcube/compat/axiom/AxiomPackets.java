package net.hollowcube.compat.axiom;

import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.packets.serverbound.*;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/// The single table of serverbound Axiom packets: which handler runs, whether API 10 is told the packet is
/// supported (a missing identifier silently disables that feature client-side), and whether it may arrive
/// through the tunnel.
@NotNullByDefault
final class AxiomPackets {

    record Definition<T extends ServerboundModPacket<T>>(
            ServerboundModPacket.Type<T> type,
            BiConsumer<Player, T> handler,
            boolean advertised,
            boolean tunneled
    ) {
        String id() {
            return type.id();
        }

        void register(PacketRegistry registry) {
            registry.register(type, handler);
        }

        T decodeTunneled(byte[] payload) throws AxiomTunnel.MalformedException {
            var buffer = NetworkBuffer.wrap(payload, 0, payload.length);
            T packet;
            try {
                packet = buffer.read(type.codec());
            } catch (RuntimeException e) {
                throw new AxiomTunnel.MalformedException("failed to decode tunneled " + id(), e);
            }
            if (buffer.readableBytes() != 0)
                throw new AxiomTunnel.MalformedException("tunneled " + id() + " left " + buffer.readableBytes() + " unread bytes");
            return packet;
        }

        void handleTunneled(Player player, byte[] payload) throws AxiomTunnel.MalformedException {
            handler.accept(player, decodeTunneled(payload));
        }
    }

    static final List<Definition<?>> SERVERBOUND = List.of(
            control(AxiomServerboundHelloPacket.TYPE, AxiomPacketHandler::onHello),
            control(AxiomServerboundTunnelPacket.TYPE, AxiomPacketHandler::onTunnel),

            tunneled(AxiomServerboundSetBlockPacket.TYPE, AxiomPacketHandler::onSetBlock),
            tunneled(AxiomServerboundSetBufferPacket.TYPE, AxiomPacketHandler::onSetBuffer),
            tunneled(AxiomServerboundSpawnEntitiesPacket.TYPE, AxiomPacketHandler::onSpawnEntities),
            tunneled(AxiomServerboundModifyEntitiesPacket.TYPE, AxiomPacketHandler::onModifyEntities),
            tunneled(AxiomServerboundEntityRequestPacket.TYPE, AxiomPacketHandler::onEntityDataRequest),

            direct(AxiomServerboundAnnotationUpdatePacket.TYPE, AxiomPacketHandler::onAnnotationUpdates),
            direct(AxiomServerboundRemoveEntitiesPacket.TYPE, AxiomPacketHandler::onRemoveEntities),
            direct(AxiomServerboundMarkerRequestPacket.TYPE, AxiomPacketHandler::onMarkerDataRequest),
            direct(AxiomServerboundSetFlySpeedPacket.TYPE, AxiomPacketHandler::onSetFlySpeed),
            direct(AxiomServerboundSetGameModePacket.TYPE, AxiomPacketHandler::onSetGameMode),
            direct(AxiomServerboundSetWorldPropertyPacket.TYPE, AxiomPacketHandler::onSetWorldProperty),
            direct(AxiomServerboundTeleportPacket.TYPE, AxiomPacketHandler::onTeleport),

            // Deliberately unsupported: API 9 clients still send these, API 10 clients are not told they exist.
            dropped(AxiomServerboundSetNoPhysicalTriggerPacket.TYPE),
            dropped(AxiomServerboundSetTimePacket.TYPE)
    );

    static final List<String> ADVERTISED = SERVERBOUND.stream()
            .filter(Definition::advertised)
            .map(Definition::id)
            .toList();

    private static final Map<String, Definition<?>> TUNNELED = new HashMap<>();

    static {
        for (var definition : SERVERBOUND)
            if (definition.tunneled()) TUNNELED.put(definition.id(), definition);
    }

    static @Nullable Definition<?> byTunnelChannel(String channel) {
        return TUNNELED.get(channel);
    }

    private static <T extends ServerboundModPacket<T>> Definition<T> control(ServerboundModPacket.Type<T> type, BiConsumer<Player, T> handler) {
        return new Definition<>(type, handler, true, false);
    }

    private static <T extends ServerboundModPacket<T>> Definition<T> tunneled(ServerboundModPacket.Type<T> type, BiConsumer<Player, T> handler) {
        return new Definition<>(type, AxiomPacketHandler.handle(handler), true, true);
    }

    private static <T extends ServerboundModPacket<T>> Definition<T> direct(ServerboundModPacket.Type<T> type, BiConsumer<Player, T> handler) {
        return new Definition<>(type, AxiomPacketHandler.handle(handler), true, false);
    }

    private static <T extends ServerboundModPacket<T>> Definition<T> dropped(ServerboundModPacket.Type<T> type) {
        return new Definition<>(type, (_, _) -> {}, false, false);
    }
}
