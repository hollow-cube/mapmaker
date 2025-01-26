package net.hollowcube.compat.impl;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@ApiStatus.Internal
public final class PacketRegistryImpl implements PacketRegistry {

    private static final Tag<Set<String>> PLAYER_CHANNELS = Tag.Transient("packets:player/channels");
    private static final String REGISTER_CHANNEL = "minecraft:register";
    private static final String UNREGISTER_CHANNEL = "minecraft:unregister";
    private static PacketRegistryImpl INSTANCE;

    private final Map<String, ServerboundModPacket.Type<?>> serverbound = new ConcurrentHashMap<>();
    private final Map<String, ClientboundModPacket.Type<?>> clientbound = new ConcurrentHashMap<>();
    private final Map<String, BiConsumer<Player, ?>> handlers = new ConcurrentHashMap<>();

    private boolean frozen = false;

    private PacketRegistryImpl(GlobalEventHandler events) {
        events.addListener(PlayerPluginMessageEvent.class, event -> {
            var player = event.getPlayer();
            var id = event.getIdentifier().intern();
            switch (id) {
                case REGISTER_CHANNEL -> player.getAndUpdateTag(PLAYER_CHANNELS, channels -> {
                    channels = Objects.requireNonNullElseGet(channels, ConcurrentHashMap::newKeySet);
                    channels.addAll(List.of(event.getMessageString().split("\0")));
                    return channels;
                });
                case UNREGISTER_CHANNEL -> player.getAndUpdateTag(PLAYER_CHANNELS, channels -> {
                    channels = Objects.requireNonNullElseGet(channels, ConcurrentHashMap::newKeySet);
                    List.of(event.getMessageString().split("\0")).forEach(channels::remove);
                    return channels;
                });
                default -> {
                    ServerboundModPacket.Type<?> type = this.serverbound.get(id);
                    if (type == null) return;
                    handlePacket(type, event);
                }
            }
        });
        events.addListener(PlayerSpawnEvent.class, event -> {
           if (!event.isFirstSpawn()) return;
           event.getPlayer().sendPluginMessage(REGISTER_CHANNEL, String.join("\0", this.serverbound.keySet()));
        });
    }

    public static PacketRegistryImpl init(GlobalEventHandler events) {
        if (INSTANCE != null) return INSTANCE;
        INSTANCE = new PacketRegistryImpl(events);
        return INSTANCE;
    }

    public void freeze() {
        this.frozen = true;
    }

    private static PacketRegistryImpl getInstance() {
        Check.stateCondition(INSTANCE == null, "PacketRegistry not initialized");
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private <T extends ServerboundModPacket<T>> void handlePacket(ServerboundModPacket.Type<T> type, PlayerPluginMessageEvent event) {
        var packet = NetworkBuffer.wrap(event.getMessage(), 0, event.getMessage().length).read(type.codec());
        var handler = (BiConsumer<Player, T>) this.handlers.get(event.getIdentifier());
        if (handler == null) return;
        handler.accept(event.getPlayer(), packet);
    }

    @Override
    public <T extends ClientboundModPacket<T>> void register(ClientboundModPacket.Type<T> type) {
        Check.stateCondition(this.frozen, "PacketRegistry is frozen");
        Check.stateCondition(isRegistered(type), "Duplicate clientbound packet type: " + type.id());

        this.clientbound.put(type.id(), type);
    }

    @Override
    public <T extends ServerboundModPacket<T>> void register(ServerboundModPacket.Type<T> type, BiConsumer<Player, T> handler) {
        Check.stateCondition(this.frozen, "PacketRegistry is frozen");
        Check.stateCondition(isRegistered(type), "Duplicate serverbound packet type: " + type.id());

        this.serverbound.put(type.id(), type);
        this.handlers.put(type.id(), handler);
    }

    public static boolean isRegistered(ClientboundModPacket.Type<?> type) {
        return getInstance().clientbound.containsKey(type.id());
    }

    public static boolean isRegistered(ServerboundModPacket.Type<?> type) {
        return getInstance().serverbound.containsKey(type.id());
    }

    public static boolean canSend(Player player, ClientboundModPacket.Type<?> type) {
        Set<String> channels = player.getTag(PLAYER_CHANNELS);
        return channels != null && channels.contains(type.id());
    }

}
