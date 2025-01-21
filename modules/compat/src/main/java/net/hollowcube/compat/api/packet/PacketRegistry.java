package net.hollowcube.compat.api.packet;

import net.minestom.server.entity.Player;

import java.util.function.BiConsumer;

public interface PacketRegistry {

    <T extends ClientboundModPacket<T>> void register(ClientboundModPacket.Type<T> type);

    <T extends ServerboundModPacket<T>> void register(ServerboundModPacket.Type<T> type, BiConsumer<Player, T> handler);

}
