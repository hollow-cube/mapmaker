package net.hollowcube.compat.impl;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketQueue {

    private static final Tag<PacketQueue> TAG = Tag.Transient("packets:queue");
    private static final int MAX_RETRIES = 20;

    private final List<ClientboundModPacket<?>> queue = new ArrayList<>();
    private final Set<String> channels = ConcurrentHashMap.newKeySet();

    private int flushCount = 0;

    public void registerChannels(@NotNull Player player, @NotNull List<String> channels) {
        this.channels.addAll(channels);
        for (String channel : channels) {
            queue.removeIf(packet -> {
                if (packet.getType().id().equals(channel)) {
                    packet.send(player, true);
                    return true;
                }
                return false;
            });
        }
    }

    public void unregisterChannels(@NotNull List<String> channels) {
        List.of(channels).forEach(this.channels::remove);

        for (String channel : channels) {
            this.queue.removeIf(packet -> packet.getType().id().equals(channel));
        }
    }

    /**
     * Tries to queue a packet for sending to the player.
     * If it can be handled by default behavior, it will return true for that to be handled by the caller.
     * ie. send the packet
     */
    public boolean send(@NotNull ClientboundModPacket<?> packet) {
        if (!this.channels.contains(packet.getType().id())) {
            if (this.flushCount < MAX_RETRIES) {
                this.queue.add(packet);
            }
            return false;
        }
        return true;
    }

    // Called once every client tick
    public void flush() {
        if (this.queue.isEmpty()) return;
        if (this.flushCount > MAX_RETRIES) return;
        if (this.flushCount == MAX_RETRIES) {
            this.queue.clear();
        }
        this.flushCount++;
    }

    public Set<String> channels() {
        return this.channels;
    }

    public static PacketQueue get(@NotNull Player player) {
        return player.updateAndGetTag(TAG, queue -> queue != null ? queue : new PacketQueue());
    }
}
