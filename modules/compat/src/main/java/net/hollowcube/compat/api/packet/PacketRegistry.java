package net.hollowcube.compat.api.packet;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;

import java.util.function.BiConsumer;

public interface PacketRegistry {

    <T extends ClientboundModPacket<T>> void register(ClientboundModPacket.Type<T> type);

    <T extends ServerboundModPacket<T>> void register(ServerboundModPacket.Type<T> type, BiConsumer<Player, T> handler);

    /**
     * Register a namespace handler, this can listen to any plugin message with the specified namespace that is not already handled by the server.
     *
     * @apiNote This should be used sparingly, it should only be used for dynamic purposes. ie. logging if a plugin message is not being handled.
     * @param namespace The namespace to listen for
     * @param handler The consumer that will be called when a plugin message is received but not already handled.
     */
    void registerNamespaceHandler(String namespace, BiConsumer<Player, PlayerPluginMessageEvent> handler);

}
