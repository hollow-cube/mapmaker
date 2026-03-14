package net.hollowcube.compat.api.packet;

import net.hollowcube.compat.impl.PacketQueue;
import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.minestom.server.Viewable;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;

import java.util.Collection;
import java.util.function.BiConsumer;

public interface ClientboundModPacket<T extends ClientboundModPacket<T>>  {

    Type<T> getType();

    default void send(Player player) {
        send(player, false);
    }

    default void sendToViewers(Collection<Player> players) {
        players.forEach(this::send);
    }

    default void sendToInstance(Instance instance) {
        sendToViewers(instance.getPlayers());
    }

    default void sendToViewers(Viewable viewable) {
        sendToViewers(viewable.getViewers());
    }

    @SuppressWarnings("unchecked")
    default void send(Player player, boolean force) {
        var type = getType();
        Check.stateCondition(!PacketRegistryImpl.isRegistered(type), "Unregistered packet type: " + type.id());
        if (!force && !PacketQueue.get(player).send(this)) return;
        player.sendPluginMessage(type.id(), NetworkBuffer.makeArray(type.codec(), (T) this));
    }

    record Type<T extends ClientboundModPacket<T>>(String id, NetworkBuffer.Type<T> codec) {

        public static <T extends ClientboundModPacket<T>> Type<T> of(String namespace, String path, NetworkBuffer.Type<T> codec) {
            return new Type<>("%s:%s".formatted(namespace, path), codec);
        }

        public static <T extends ClientboundModPacket<T>> Type<T> of(String namespace, String path, BiConsumer<NetworkBuffer, T> writer) {
            var type = new NetworkBuffer.Type<T>() {
                @Override
                public void write(NetworkBuffer buffer, T value) {
                    writer.accept(buffer, value);
                }

                @Override
                public T read(NetworkBuffer buffer) {
                    throw new UnsupportedOperationException("You cannot read a write-only packet type");
                }
            };
            return new Type<>("%s:%s".formatted(namespace, path), type);
        }
    }
}
