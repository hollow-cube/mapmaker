package net.hollowcube.mapmaker.chat;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.chat.components.MessageComponents;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.event.PunishmentCreatedEvent;
import net.hollowcube.mapmaker.punishments.event.PunishmentRevokedEvent;
import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ChatMessageData;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.listener.manager.PacketPlayListenerConsumer;
import net.minestom.server.message.ChatMessageType;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ChatMessageListener implements Closeable, PacketPlayListenerConsumer<ClientChatMessagePacket> {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageListener.class);

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private static final String CHAT_STREAM = "CHAT_PROCESSED";
    private static final ConsumerConfiguration CHAT_CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("chat.processed.>")
        .deliverPolicy(DeliverPolicy.New)
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    private static final Sound TAG_DING = Sound.sound()
        .type(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP)
        .source(Sound.Source.PLAYER)
        .volume(5)
        .build();

    private static final Tag<Long> LAST_CHAT_MESSAGE = Tag.Long("last_chat_message").defaultValue(0L);
    private static final long CHAT_COOLDOWN = 500L;

    private final SessionManager sessionManager;
    private final PlayerService playerService;
    private final MapService mapService;
    private final PunishmentService punishmentService;
    private final JetStreamWrapper jetStream;

    private final AsyncLoadingCache<String, Optional<Punishment>> playerMuteCache;
    private final MessageComponents components;

    private final MessageConsumer consumer;

    public ChatMessageListener(
        SessionManager sessionManager, PlayerService playerService,
        MapService mapService, PunishmentService punishmentService,
        JetStreamWrapper jetStream
    ) {
        this.sessionManager = sessionManager;
        this.playerService = playerService;
        this.mapService = mapService;
        this.punishmentService = punishmentService;
        this.jetStream = jetStream;

        this.playerMuteCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .executor(FutureUtil.VIRTUAL)
            .buildAsync(this::fetchActiveMute);

        this.components = new MessageComponents(mapService, playerService);

        MinecraftServer.getGlobalEventHandler()
            .addListener(PunishmentCreatedEvent.class, this::handlePunishmentCreated)
            .addListener(PunishmentRevokedEvent.class, this::handlePunishmentRevoked);

        this.consumer = jetStream.subscribe(CHAT_STREAM, CHAT_CONSUMER_CONFIG, ChatMessageData.class, this::handleChatMessage);
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(ClientChatMessagePacket packet, Player player) {
        final String message = packet.message();
        if (!Messenger.canReceiveMessage(player)) {
            Messenger.sendRejectionMessage(player);
            return;
        }

        var playerData = PlayerData.fromPlayer(player);
        String channel = playerData.getSetting(PlayerSettings.CHAT_CHANNEL);

        if (!ClientChatMessageData.CHANNEL_STAFF.equals(channel) && sessionManager.isHidden(playerData.id())) {
            player.sendMessage(Component.text("you cannot chat while vanished"));
            return;
        }

        long messageSeed = ThreadLocalRandom.current().nextLong();
        FutureUtil.submitVirtual(() -> {
            String currentMapId = null;
            if (message.contains("[map]")) {
                try {
                    var currentMap = MiscFunctionality.getCurrentMap(sessionManager, mapService, player);
                    if (currentMap == null || !currentMap.isPublished()) {
                        player.sendMessage(Component.translatable("chat.map.invalid"));
                        return;
                    }
                    currentMapId = currentMap.id();
                } catch (MapService.NotFoundError _) {
                    player.sendMessage(Component.translatable("chat.map.invalid"));
                    return;
                }
            }

            trySendChatMessage(
                player,
                new ClientChatMessageData(ClientChatMessageData.Type.CHAT_UNSIGNED, playerData.id(), message, channel, currentMapId, messageSeed)
            );
        });
    }

    @Blocking
    public void trySendChatMessage(Player sender, ClientChatMessageData message) {
        // Do not do anything if the player is muted
        if (testMuteState(sender)) return;

        long now = System.currentTimeMillis();
        if (now - sender.getTag(LAST_CHAT_MESSAGE) < CHAT_COOLDOWN) {
            sender.sendMessage(Component.translatable("chat.cooldown"));
            return;
        }
        sender.setTag(LAST_CHAT_MESSAGE, now);

        // For now stick everything in .global, may split it up in the future but would have
        // to resolve reply targets in order to do it properly.
        this.jetStream.publish("chat.raw.global", message);
    }

    // True if muted but message has already been sent
    @Blocking
    private boolean testMuteState(Player player) {
        try {
            var playerId = PlayerData.fromPlayer(player).id();
            var mute = playerMuteCache.get(playerId).get(3, TimeUnit.SECONDS);
            if (mute.isPresent()) {
                player.sendMessage(Component.translatable("punishment.muted"));
                return true;
            }

            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleChatMessage(Message msg, ChatMessageData message) {
        switch (message.type()) {
            case CHAT_UNSIGNED -> FutureUtil.submitVirtual(() -> {
                switch (message.channel()) {
                    case ClientChatMessageData.CHANNEL_GLOBAL -> handleUnsignedChat(
                        message, "chat.channel.global",
                        _ -> true
                    );
                    case ClientChatMessageData.CHANNEL_LOCAL -> {
                        var senderMap = OpUtils.map(sessionManager.getPresence(message.sender()), Presence::mapId);
                        if (senderMap == null) return; // Can't find the sender's map
                        handleUnsignedChat(message, "chat.channel.local", recipient -> {
                            var recipientMap = OpUtils.map(sessionManager.getPresence(recipient.getUuid().toString()), Presence::mapId);
                            return Objects.equals(senderMap, recipientMap);
                        });
                    }
                    case ClientChatMessageData.CHANNEL_STAFF -> handleUnsignedChat(
                        message, "chat.channel.staff",
                        recipient -> {
                            var playerData = PlayerData.fromPlayer(recipient);
                            var isStaffMode = playerData.getSetting(PlayerSettings.STAFF_MODE);
                            return isStaffMode && playerData.has(Permission.GENERIC_STAFF);
                        }
                    );
                    default -> handleDirectMessage(message);
                }
            });
            case CHAT_SYSTEM -> handleChatSystem(message);
        }
    }

    @Blocking
    protected void handleUnsignedChat(ChatMessageData message, String key, Predicate<Player> filter) {
        logger.info("Received chat message: {}", message);

        try {
            var senderDisplayName = playerService.getPlayerDisplayName2(message.sender());
            var senderName = senderDisplayName.build(DisplayName.Context.DEFAULT);
            var isColored = senderDisplayName.parts().size() > 1;

            for (var recipient : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                if (recipient.getSettings().chatMessageType() != ChatMessageType.FULL) {
                    // Recipient has disabled chat messages - they only want system messages
                    continue;
                }

                var isSender = recipient.getUuid().toString().equals(message.sender());
                if (!filter.test(recipient)) continue;

                var data = this.components.createGlobalMessage(recipient, message);
                var shouldPing = PlayerData.fromPlayer(recipient).getSetting(PlayerSettings.ENABLE_PING_SOUNDS);
                if (data.ping() && shouldPing) recipient.playSound(TAG_DING);

                var text = data.text().color(isColored ? NamedTextColor.WHITE : NamedTextColor.GRAY);

                recipient.sendMessage(Component.translatable(key, senderName, text));

                if (isSender) {
                    data.extra().values().forEach(recipient::sendMessage);
                }
            }

            // If there is an extra message, handle it
            if (message.extra() != null && message.extra().type() == ClientChatMessageData.Type.CHAT_SYSTEM) {
                handleChatSystem(message.extra());
            }
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        }
    }

    @Blocking
    private void handleDirectMessage(ChatMessageData message) {
        try {
            var sender = CONNECTION_MANAGER.getOnlinePlayerByUuid(UUID.fromString(message.sender()));
            var target = CONNECTION_MANAGER.getOnlinePlayerByUuid(UUID.fromString(message.channel()));
            var spies = new ArrayList<Player>(); // People spying todo

            if (sender == null && target == null && spies.isEmpty()) return; // Not relevant to this server

            var targetDisplayName = playerService.getPlayerDisplayName2(message.channel()).build();
            var senderDisplayName = playerService.getPlayerDisplayName2(message.sender()).build();

            if (target != null) {
                var data = this.components.createDirectMessage(target, message);
                target.playSound(TAG_DING);
                target.sendMessage(Component.translatable(
                    "chat.channel.dm.receive", List.of(senderDisplayName, targetDisplayName, data.text())
                ));
            }
            if (sender != null) {
                var data = this.components.createDirectMessage(sender, message);
                sender.sendMessage(Component.translatable(
                    "chat.channel.dm.send", List.of(senderDisplayName, targetDisplayName, data.text())
                ));
                data.extra().values().forEach(sender::sendMessage);
            }
            for (var spy : spies) {
                var data = this.components.createDirectMessage(spy, message);
                spy.sendMessage(Component.translatable(
                    "chat.channel.dm.spy", List.of(senderDisplayName, targetDisplayName, data.text())
                ));
            }
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        }
    }

    @Blocking
    protected void handleChatSystem(ChatMessageData message) {
        var player = MinecraftServer.getConnectionManager()
            .getOnlinePlayerByUuid(UUID.fromString(message.target()));
        if (player == null) return; // Not relevant to this server
        if (message.respectClientSettings() && player.getSettings().chatMessageType() != ChatMessageType.FULL) {
            // Recipient has disabled chat messages and this message is sent only on FULL
            return;
        }

        try {
            var args = new ArrayList<Component>();
            for (var rawArg : message.argsSafe()) {
                // Hacky way to send display name in arg, we should properly support a type field on arg to resolve it.
                if (rawArg.startsWith("pdn::")) {
                    var displayName = playerService.getPlayerDisplayName2(rawArg.substring(5));
                    args.add(displayName.build());
                    continue;
                }
                args.add(Component.text(rawArg));
            }
            player.sendMessage(Component.translatable(message.key(), args));
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
        }
    }

    private CompletableFuture<Optional<Punishment>> fetchActiveMute(String playerId, Executor executor) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(punishmentService.getActivePunishment(playerId, PunishmentType.MUTE)), executor);
    }

    private void handlePunishmentCreated(PunishmentCreatedEvent event) {
        if (event.punishment().type() != PunishmentType.MUTE) return;
        this.playerMuteCache.put(event.punishment().playerId(), CompletableFuture.completedFuture(Optional.of(event.punishment())));
    }

    private void handlePunishmentRevoked(PunishmentRevokedEvent event) {
        if (event.punishment().type() != PunishmentType.MUTE) return;
        this.playerMuteCache.put(event.punishment().playerId(), CompletableFuture.completedFuture(Optional.empty()));
    }

}
