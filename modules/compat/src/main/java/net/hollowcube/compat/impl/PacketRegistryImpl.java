package net.hollowcube.compat.impl;

import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@ApiStatus.Internal
public final class PacketRegistryImpl implements PacketRegistry {

    private static final String REGISTER_CHANNEL = "minecraft:register";
    private static final String UNREGISTER_CHANNEL = "minecraft:unregister";
    private static PacketRegistryImpl INSTANCE;

    private final Map<String, ServerboundModPacket.Type<?>> serverbound = new ConcurrentHashMap<>();
    private final Map<String, ClientboundModPacket.Type<?>> clientbound = new ConcurrentHashMap<>();

    private final Map<String, BiConsumer<Player, ?>> handlers = new ConcurrentHashMap<>();
    private final Map<String, BiConsumer<Player, PlayerPluginMessageEvent>> namespaceHandlers = new ConcurrentHashMap<>();

    private boolean frozen = false;

    private PacketRegistryImpl(GlobalEventHandler events) {
        events.addListener(PlayerPluginMessageEvent.class, event -> {
            var player = event.getPlayer();
            var id = event.getIdentifier().intern();
            switch (id) {
                case REGISTER_CHANNEL -> {
                    var channels = new ArrayList<>(Arrays.asList(event.getMessageString().split("\0")));
                    var registerEvent = new ModChannelRegisterEvent(player, channels);
                    EventDispatcher.call(registerEvent);
                    PacketQueue.get(player).registerChannels(player, channels);
                    if (!registerEvent.getDisabledMods().isEmpty() && Boolean.TRUE.equals(player.getAndSetTag(CompatProvider.FIRST_JOIN_TAG, null))) {
                        var pvnText = Component.text(ProtocolVersions.getProtocolName(MinecraftServer.PROTOCOL_VERSION));
                        var disabledModList = Component.join(JoinConfiguration.commas(true), registerEvent.getDisabledMods());
                        System.out.println(disabledModList);
                        player.sendMessage(Component.translatable("join.unsupported_mods", pvnText, disabledModList));
                    }
                }
                case UNREGISTER_CHANNEL -> {
                    var channels = Arrays.asList(event.getMessageString().split("\0"));
                    PacketQueue.get(player).unregisterChannels(channels);
                }
                default -> {
                    ServerboundModPacket.Type<?> type = this.serverbound.get(id);
                    if (type == null) {
                        var namspace = id.split(":", 2)[0];
                        var handler = this.namespaceHandlers.get(namspace);
                        if (handler != null) {
                            handler.accept(player, event);
                        }
                    } else {
                        handlePacket(type, event);
                    }
                }
            }
        });
        events.addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;
            event.getPlayer().sendPluginMessage(REGISTER_CHANNEL, String.join("\0", this.serverbound.keySet()));
        });
        events.addListener(PlayerTickEndEvent.class, event -> PacketQueue.get(event.getPlayer()).flush());
    }

    public static PacketRegistryImpl init(GlobalEventHandler events) {
        if (INSTANCE != null) return INSTANCE;
        INSTANCE = new PacketRegistryImpl(events);
        return INSTANCE;
    }

    @TestOnly
    public static void unsafeReset() {
        INSTANCE = null;
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

    @Override
    public void registerNamespaceHandler(String namespace, BiConsumer<Player, PlayerPluginMessageEvent> handler) {
        Check.stateCondition(this.frozen, "PacketRegistry is frozen");
        Check.stateCondition(this.namespaceHandlers.containsKey(namespace), "Duplicate namespace handler: " + namespace);

        this.namespaceHandlers.put(namespace, handler);
    }

    public static boolean isRegistered(ClientboundModPacket.Type<?> type) {
        return getInstance().clientbound.containsKey(type.id());
    }

    public static boolean isRegistered(ServerboundModPacket.Type<?> type) {
        return getInstance().serverbound.containsKey(type.id());
    }

}
