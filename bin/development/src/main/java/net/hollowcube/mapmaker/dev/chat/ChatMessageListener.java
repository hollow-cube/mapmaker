package net.hollowcube.mapmaker.dev.chat;

import com.google.gson.Gson;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.dev.DevServer;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.kafka.FriendlyProducer;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.temp.ChatMessageData;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.listener.manager.PacketPlayListenerConsumer;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class ChatMessageListener extends BaseConsumer<ChatMessageData> implements PacketPlayListenerConsumer<ClientChatMessagePacket> {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageListener.class);

    private static final String CHAT_TOPIC = "chat";
    private static final String CHAT_OUT_TOPIC = "chat-messages";
    private static final Gson GSON = AbstractHttpService.GSON;

    private final PlayerService playerService;
    private final MapService mapService;
    private final FriendlyProducer producer;

    public ChatMessageListener(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull String kafkaBrokers) {
        super(CHAT_OUT_TOPIC, "chat", ChatMessageListener::fromJson, kafkaBrokers);
        this.playerService = playerService;
        this.mapService = mapService;
        this.producer = new FriendlyProducer(kafkaBrokers);

        setAutocommit(false);
    }

    private static @NotNull ChatMessageData fromJson(@NotNull String json) {
        return GSON.fromJson(json, ChatMessageData.class);
    }

    @Override
    public void close() {
        super.close();
        this.producer.close();
    }

    @Override
    public void accept(ClientChatMessagePacket packet, Player player) {
        final String message = packet.message();
        if (!Messenger.canReceiveMessage(player)) {
            Messenger.sendRejectionMessage(player);
            return;
        }

        var currentMap = MapWorld.forPlayerOptional(player);
        if ((currentMap == null || !currentMap.map().isPublished()) && message.contains("[map]")) {
            player.sendMessage(Component.text("You are not in a published map.")); //todo message
            return;
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        var messageData = new ClientChatMessageData(ClientChatMessageData.Type.CHAT_UNSIGNED,
                playerData.id(), message, "global", currentMap == null ? null : currentMap.map().id());
        logger.info("{}: {}", playerData.username(), messageData);
        this.producer.produceAndForget(CHAT_TOPIC, GSON.toJson(messageData));
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull ChatMessageData message) {
        Thread.startVirtualThread(() -> {
            logger.info("Received chat message: {}", message);
            try {
                // This is braindead inefficient
                // Awful garbage code

                var senderDisplyName = playerService.getPlayerDisplayName2(message.sender());
                var sender = senderDisplyName.build(DisplayName.Context.DEFAULT);
                var key = senderDisplyName.parts().size() > 1 ? "chat.channel.global.white" : "chat.channel.global.default";

                var maps = new HashMap<String, MapData>();

                for (var recipient : MinecraftServer.getConnectionManager().getPlayers(ConnectionState.PLAY)) {
                    var builder = Component.text();

                    boolean hasDing = false;
                    for (var part : message.parts()) {
                        switch (part.type()) {
                            case RAW -> {
                                Component component = Component.text(part.text());

                                var namePattern = Pattern.compile(String.format("(?:^|\\s)(%s)", recipient.getUsername()), Pattern.CASE_INSENSITIVE);
                                if (namePattern.matcher(part.text()).find()) {
                                    if (!hasDing && !recipient.getUuid().toString().equals(message.sender()))
                                        recipient.playSound(DevServer.TAG_DING);
                                    hasDing = true;
                                    component = component.replaceText(TextReplacementConfig.builder()
                                            .match(namePattern)
                                            .replacement((match, unused) -> Component.text(match.group(), TextColor.color(0xffe59e)))
                                            .build());
                                }

                                builder.append(component);
                            }
                            case EMOJI -> {
                                var emoji = Emoji.findByName(part.name());
                                if (emoji == null) {
                                    builder.append(Component.text(":" + part.name() + ":"));
                                } else builder.append(emoji.component());
                            }
                            case MAP -> {
                                var map = maps.computeIfAbsent(part.mapId(), mapId -> mapService.getMap(message.sender(), mapId));
                                builder.append(MapData.createMapHoverText(map));
                            }
                        }
                    }

                    recipient.sendMessage(Component.translatable(key, sender, builder.build()));
                }
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        });
    }

}
