package net.hollowcube.compat.api.packet;

import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;

public interface ClientboundModPacket<T extends ClientboundModPacket<T>>  {

    Type<T> getType();

    default void send(Player player) {
        send(player, false);
    }

    @SuppressWarnings("unchecked")
    default void send(Player player, boolean force) {
        var type = getType();
        Check.stateCondition(!PacketRegistryImpl.isRegistered(type), "Unregistered packet type: " + type.id());
        if (!PacketRegistryImpl.canSend(player, type) && !force) return;
        player.sendPluginMessage(type.id(), NetworkBuffer.makeArray(type.codec(), (T) this));
    }

    record Type<T extends ClientboundModPacket<T>>(String id, NetworkBuffer.Type<T> codec) {

        public static <T extends ClientboundModPacket<T>> Type<T> of(String namespace, String path, NetworkBuffer.Type<T> codec) {
            return new Type<>("%s:%s".formatted(namespace, path), codec);
        }
    }
}
