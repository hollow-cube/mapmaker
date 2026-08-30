package net.hollowcube.mapmaker.chat;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.chat.ChatChannel;
import net.hollowcube.ipc.chat.ChatMessage;
import net.hollowcube.ipc.chat.ChatResult;
import net.hollowcube.ipc.util.IpcException;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.chat.components.MessageComponents;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.NumberUtil;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/// Both ends of chat on a game server: what a player types goes to the api, and what the api
/// publishes is rendered for whoever here should see it.
///
/// Everything between those two — stripping, mutes, direct message settings, the filter, the log,
/// splitting the message into parts — happens in the api now, and the answer comes back as a
/// [ChatResult] this renders. Nothing is published from here.
public class ChatMessageListener implements Closeable, PacketPlayListenerConsumer<ClientChatMessagePacket> {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageListener.class);

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    /// The stream the Go handler published on, consumed alongside [ChatMessage#SUBJECT] until no
    /// server old enough to send its chat that way is running.
    private static final String LEGACY_STREAM = "CHAT_PROCESSED";
    private static final ConsumerConfiguration LEGACY_CONSUMER_CONFIG = ConsumerConfiguration.builder()
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
    private final ApiClient api;
    /// The map a player is in, or null outside one. Supplied because the worlds live above this
    /// module; it is what both the sender's map and each recipient's is read from.
    private final Function<Player, @Nullable String> mapOf;

    private final MessageComponents components;

    private final Closeable consumer;
    private final MessageConsumer legacyConsumer;

    public ChatMessageListener(
        SessionManager sessionManager, ApiClient api, JetStreamWrapper jetStream,
        Function<Player, @Nullable String> mapOf
    ) {
        this.sessionManager = sessionManager;
        this.api = api;
        this.mapOf = mapOf;
        this.components = new MessageComponents(api);

        this.consumer = jetStream.subscribe(ChatMessage.SUBJECT, Wire.gson(), ChatMessage.class,
            (_, message) -> FutureUtil.submitVirtual(() -> deliver(message)));
        this.legacyConsumer = jetStream.subscribe(LEGACY_STREAM, LEGACY_CONSUMER_CONFIG, LegacyChatMessage.class,
            this::handleLegacyChatMessage);
    }

    @Override
    public void close() throws IOException {
        // Both, whatever the first one does: leaving the legacy consumer open because the new one
        // objected would keep this server reading chat after it stopped serving it.
        try (consumer; legacyConsumer) {
            logger.debug("closing chat consumers");
        } catch (Exception e) {
            throw new IOException(e);
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
        var channel = ChatChannels.of(playerData.getSetting(PlayerSettings.CHAT_CHANNEL));

        if (channel != ChatChannel.STAFF && sessionManager.isHidden(playerData.id())) {
            player.sendMessage(Component.text("you cannot chat while vanished"));
            return;
        }

        FutureUtil.submitVirtual(() -> trySendChatMessage(player, channel, null, message));
    }

    /// Sends one message and tells the sender what became of it.
    ///
    /// @param targetId who a [ChatChannel#DIRECT] is for; the api resolves a reply itself
    @Blocking
    public void trySendChatMessage(Player sender, ChatChannel channel, @Nullable String targetId,
                                   String message) {
        long now = System.currentTimeMillis();
        if (now - sender.getTag(LAST_CHAT_MESSAGE) < CHAT_COOLDOWN) {
            sender.sendMessage(Component.translatable("chat.cooldown"));
            return;
        }
        sender.setTag(LAST_CHAT_MESSAGE, now);

        ChatResult result;
        try {
            // The map goes whether or not they wrote `[map]`: it is what places a local message, and
            // the api is what decides whether the map is one anyone else could open.
            result = api.chat.send(PlayerData.fromPlayer(sender).id(), ServerRuntime.getRuntime().hostname(),
                channel, targetId, message, mapOf.apply(sender));
        } catch (IpcException e) {
            ExceptionReporter.reportException(e, sender);
            sender.sendMessage(Component.translatable("generic.unknown_error"));
            return;
        }

        switch (result) {
            // It went out, or there was nothing left to send; neither is worth a message.
            case ChatResult.Sent _ -> {
            }
            case ChatResult.Muted muted -> sender.sendMessage(muted.expiresAt() == null
                ? Component.translatable("punishment.muted")
                : Component.translatable("punishment.muted.until",
                    Component.text(NumberUtil.formatTimeUntil(muted.expiresAt()))));
            case ChatResult.Censored _ -> sender.sendMessage(Component.translatable("chat.censored"));
            // No target to name means there was nobody to reply to in the first place.
            case ChatResult.TargetOffline offline -> sender.sendMessage(offline.targetId() == null
                ? Component.translatable("chat.msg.no_reply")
                : Component.translatable("generic.player.offline", displayName(offline.targetId())));
            case ChatResult.DmDisabled disabled -> sender.sendMessage(disabled.targetId() == null
                ? Component.translatable("chat.channel.dm.disabled.self")
                : Component.translatable("chat.channel.dm.disabled", displayName(disabled.targetId())));
            case ChatResult.MapNotPublished _ ->
                sender.sendMessage(Component.translatable("generic.map.chat.usage"));
            case ChatResult.Unknown unknown -> {
                // An api that knows a reason this build does not. Saying nothing would look like the
                // message went out.
                logger.warn("unknown chat result from the api: {}", unknown.type());
                sender.sendMessage(Component.translatable("generic.unknown_error"));
            }
        }
    }

    private Component displayName(String playerId) {
        return api.players.getDisplayName(playerId).build();
    }

    private void handleLegacyChatMessage(Message msg, LegacyChatMessage legacy) {
        var message = legacy.toChatMessage();
        if (message == null) return;
        FutureUtil.submitVirtual(() -> deliver(message));
    }

    @Blocking
    private void deliver(ChatMessage message) {
        switch (message.channel()) {
            case GLOBAL -> handleUnsignedChat(message, "chat.channel.global", _ -> true);
            case LOCAL -> handleLocalChat(message);
            case STAFF -> handleUnsignedChat(message, "chat.channel.staff", recipient -> {
                var playerData = PlayerData.fromPlayer(recipient);
                return playerData.getSetting(PlayerSettings.STAFF_MODE) && playerData.has(Permission.GENERIC_STAFF);
            });
            // A reply is resolved to a direct message before it is published; this is only here
            // because the compiler is right to ask.
            case DIRECT, REPLY -> handleDirectMessage(message);
            case UNKNOWN -> logger.warn("dropped a chat message on a channel this build does not know");
        }
    }

    @Blocking
    private void handleLocalChat(ChatMessage message) {
        var senderMap = message.mapId();
        if (senderMap != null) {
            handleUnsignedChat(message, "chat.channel.local",
                recipient -> senderMap.equals(mapOf.apply(recipient)));
            return;
        }

        // From a server old enough to publish through the go path, which carried no map with it. The
        // session service's view of where everyone is, as those servers do it. Delete with the
        // legacy consumer.
        var presenceMap = OpUtils.map(sessionManager.getPresence(message.senderId()), Presence::mapId);
        if (presenceMap == null) return;
        handleUnsignedChat(message, "chat.channel.local", recipient -> Objects.equals(presenceMap,
            OpUtils.map(sessionManager.getPresence(recipient.getUuid().toString()), Presence::mapId)));
    }

    @Blocking
    protected void handleUnsignedChat(ChatMessage message, String key, Predicate<Player> filter) {
        logger.info("Received chat message: {}", message);

        try {
            var senderDisplayName = api.players.getDisplayName(message.senderId());
            var senderName = senderDisplayName.build();
            var isColored = senderDisplayName.parts().size() > 1;

            for (var recipient : CONNECTION_MANAGER.getOnlinePlayers()) {
                if (recipient.getSettings().chatMessageType() != ChatMessageType.FULL) {
                    // Recipient has disabled chat messages - they only want system messages
                    continue;
                }

                var isSender = recipient.getUuid().toString().equals(message.senderId());
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
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        }
    }

    @Blocking
    private void handleDirectMessage(ChatMessage message) {
        var targetId = message.targetId();
        if (targetId == null) {
            logger.warn("dropped a direct message with no target");
            return;
        }

        try {
            var sender = CONNECTION_MANAGER.getOnlinePlayerByUuid(UUID.fromString(message.senderId()));
            var target = CONNECTION_MANAGER.getOnlinePlayerByUuid(UUID.fromString(targetId));
            var spies = new ArrayList<Player>(); // People spying todo

            if (sender == null && target == null && spies.isEmpty()) return; // Not relevant to this server

            var targetDisplayName = api.players.getDisplayName(targetId).build();
            var senderDisplayName = api.players.getDisplayName(message.senderId()).build();

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
}
