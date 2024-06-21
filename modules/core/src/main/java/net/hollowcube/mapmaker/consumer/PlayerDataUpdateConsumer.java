package net.hollowcube.mapmaker.consumer;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataUpdateMessage;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

public class PlayerDataUpdateConsumer extends BaseConsumer<PlayerDataUpdateMessage> {
    private static final Logger logger = LoggerFactory.getLogger(PlayerDataUpdateConsumer.class);

    private static final String TOPIC_NAME = "player_data_updates";
    private static final String GROUP_ID = ServerRuntime.getRuntime().hostname();

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    private final PlayerService playerService;

    public PlayerDataUpdateConsumer(@NotNull String bootstrapServers, @NotNull PlayerService playerService) {
        super(TOPIC_NAME, GROUP_ID, s -> AbstractHttpService.GSON.fromJson(s, PlayerDataUpdateMessage.class), bootstrapServers);

        this.playerService = playerService;
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull PlayerDataUpdateMessage message) {
        logger.info("Received player data update message {} for {}", message.action(), message.id());
        switch (message.action()) {
            case MODIFY -> handlePlayerDataModify(message);
        }
    }

    private void handlePlayerDataModify(@NotNull PlayerDataUpdateMessage message) {
        var player = CONNECTION_MANAGER.getOnlinePlayerByUuid(UUID.fromString(message.id()));
        if (player == null) return;

        if (message.backpack() != null) {
            var backpack = PlayerBackpack.fromPlayer(player);
            backpack.update(message.backpack());
            backpack.refresh();
        }

        // todo message.experience
        // todo message.hypercubeExp

        var playerData = PlayerDataV2.fromPlayer(player);
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
