package net.hollowcube.mapmaker.consumer;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerDataUpdateMessage;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public class PlayerDataUpdateConsumer implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(PlayerDataUpdateConsumer.class);

    private static final String STREAM = "PLAYER_DATA_MANAGEMENT";
    private static final ConsumerConfiguration CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("player-data.>")
        .deliverPolicy(DeliverPolicy.New)
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    private final PlayerService playerService;

    private final MessageConsumer consumer;

    public PlayerDataUpdateConsumer(@NotNull PlayerService playerService, JetStreamWrapper jetStream) {
        this.playerService = playerService;

        this.consumer = jetStream.subscribe(STREAM, CONSUMER_CONFIG, PlayerDataUpdateMessage.class, this::handlePlayerDataUpdate);
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handlePlayerDataUpdate(@NotNull Message msg, @NotNull PlayerDataUpdateMessage message) {
        logger.info("Received player data update message {} for {}", message.action(), message.id());
        switch (message.action()) {
            case MODIFY -> handlePlayerDataModify(message);
        }
    }

    private void handlePlayerDataModify(@NotNull PlayerDataUpdateMessage message) {
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(message.id()));
        if (player == null) return;

        if (message.backpack() != null) {
            var backpack = PlayerBackpack.fromPlayer(player);
            backpack.update(message.backpack());
            backpack.refresh();
        }

        // todo message.experience
        // todo message.hypercubeExp

        var playerData = PlayerData.fromPlayer(player);
        if (message.coins() != null) playerData.setCoins(message.coins());
        if (message.cubits() != null) playerData.setCubits(message.cubits());

        // If the message contained a reason, we need to send the player a message about it.
        if (message.reason() != null) sendPlayerUpdateReasonMessage(player, message.reason());
    }

    private static final int MINUTES_TO_MONTHS = 31 * 24 * 60;

    private void sendPlayerUpdateReasonMessage(@NotNull Player player, @NotNull PlayerDataUpdateMessage.Reason reason) {
        switch (reason.type()) {
            case CUBITS -> {
                player.sendMessage(Component.translatable("store.confirmation.cubits", Component.text(reason.quantity())));

                var displayName = playerService.getPlayerDisplayName2(player.getUuid().toString()).build(DisplayName.Context.DEFAULT);
                Audiences.all().sendMessage(Component.translatable("store.broadcast.cubits", displayName));
            }
            case HYPERCUBE -> {
                var months = Component.text(reason.quantity() / MINUTES_TO_MONTHS);
                player.sendMessage(Component.translatable("store.confirmation.hypercube", months));

                var displayName = playerService.getPlayerDisplayName2(player.getUuid().toString()).build(DisplayName.Context.DEFAULT);
                Audiences.all().sendMessage(Component.translatable("store.broadcast.hypercube", displayName));

                //todo need to refresh the players display name everywhere, it probably changed.
            }
            case VOTE -> {
                var source = Component.text(Objects.requireNonNull(reason.voteSource()));
                var reward = createVoteRewardComponent(Objects.requireNonNull(reason.relativeUpdate()));
                player.sendMessage(Component.translatable("store.confirmation.vote", source, reward));
            }
        }
    }

    private @NotNull Component createVoteRewardComponent(@NotNull PlayerDataUpdateMessage relative) {
        if (relative.backpack() != null) {
            return Component.text("some backpack update todo");
        } else if (relative.coins() != null) {
            return Component.translatable("quantity.coins", Component.text(relative.coins()));
        } else if (relative.cubits() != null) {
            return Component.translatable("quantity.cubits", Component.text(relative.cubits()));
        } else {
            throw new IllegalStateException("Unexpected empty vote reward: " + relative);
        }
    }
}
