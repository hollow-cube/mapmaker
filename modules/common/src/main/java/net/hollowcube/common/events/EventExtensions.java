package net.hollowcube.common.events;

import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.client.play.*;

public final class EventExtensions {

    private EventExtensions() {
    }

    public static void init() {
        var packets = MinecraftServer.getPacketListenerManager();

        packets.setPlayListener(ClientCreativeInventoryActionPacket.class, PlayerGiveCreativeItemEvent::post);
        packets.setPlayListener(ClientVehicleMovePacket.class, PlayerMoveVehicleEvent::post);
        packets.setPlayListener(ClientUpdateSignPacket.class, UpdateSignTextEvent::post);
        packets.setPlayListener(ClientStatusPacket.class, RequestStatsEvent::post);
        packets.setPlayListener(ClientPlayerDiggingPacket.class, PlayerHitBlockEvent::post);
    }
}
