package net.hollowcube.terraform.compat.axiom.packet;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AxiomPacketRegistry<T> {
    public record PacketInfo<P>(Class<P> packetClass, String channel, NetworkBuffer.Type<P> serializer) {
    }

    private final Map<String, PacketInfo<? extends T>> suppliers = new HashMap<>();
    private final ClassValue<PacketInfo<T>> packetIds = new ClassValue<>() {
        @SuppressWarnings("unchecked") @Override
        protected PacketInfo<T> computeValue(@NotNull Class<?> type) {
            for (PacketInfo<? extends T> info : suppliers.values()) {
                if (info != null && info.packetClass == type) {
                    return (PacketInfo<T>) info;
                }
            }
            throw new IllegalStateException("Packet type " + type + " isn't registered!");
        }
    };

    protected AxiomPacketRegistry() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <P> void register(String channel, Class<? extends P> packetClass, NetworkBuffer.Type<P> serializer) {
        suppliers.put(channel, (PacketInfo<? extends T>) new PacketInfo<P>((Class) packetClass, channel, serializer));
    }

    public @NotNull PacketInfo<T> packetInfo(@NotNull Class<?> packetClass) {
        return packetIds.get(packetClass);
    }

    @SuppressWarnings("unchecked")
    public @Nullable NetworkBuffer.Type<T> packetInfo(@NotNull String channel) {
        var entry = suppliers.get(channel);
        return (NetworkBuffer.Type<T>) (entry == null ? null : entry.serializer);
    }

    public @NotNull Collection<String> channels() {
        return suppliers.keySet();
    }

}
