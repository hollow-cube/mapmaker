package net.hollowcube.common.events;

import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.client.play.ClientCreativeInventoryActionPacket;
import net.minestom.server.network.packet.client.play.ClientSelectBundleItemPacket;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;

public final class EventExtensions {

    private EventExtensions() {
    }

    public static void init() {
        var packets = MinecraftServer.getPacketListenerManager();

        packets.setPlayListener(ClientCreativeInventoryActionPacket.class, PlayerGiveCreativeItemEvent::post);
        packets.setPlayListener(ClientVehicleMovePacket.class, PlayerMoveVehicleEvent::post);
        packets.setPlayListener(ClientUpdateSignPacket.class, UpdateSignTextEvent::post);
        packets.setPlayListener(ClientSelectBundleItemPacket.class, SelectBundleSlotEvent::post);
    }
}
