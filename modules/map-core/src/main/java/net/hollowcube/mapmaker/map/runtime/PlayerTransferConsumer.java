package net.hollowcube.mapmaker.map.runtime;

import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;

public class PlayerTransferConsumer implements Closeable {

    @RuntimeGson
    public record Message(
        @NotNull String playerId,
        @Nullable String from,
        @NotNull String to,
        @NotNull ServerBridge.JoinMapState state
    ) {
    }

    public static final String STREAM_NAME = "PLAYER_MANAGEMENT";

    private final ServerBridge bridge;
    private final MessageConsumer consumer;

    public PlayerTransferConsumer(@NotNull ServerBridge bridge, @NotNull JetStreamWrapper jetStream) {
        this.bridge = bridge;

        // We must initialize this at runtime so its not initialized in the graal build,
        // aka giving us every message since build on start :)
        var consumerConfig = ConsumerConfiguration.builder()
            .filterSubjects("player.transfer")
            // Process anything 1m prior in case we are starting as the map is drained/deleted
            .deliverPolicy(DeliverPolicy.ByStartTime)
            .startTime(ZonedDateTime.now().minus(Duration.ofMinutes(1)))
            .ackPolicy(AckPolicy.None)
            .inactiveThreshold(Duration.ofMinutes(5))
            .build();
        this.consumer = jetStream.subscribe(STREAM_NAME, consumerConfig, Message.class, this::handleTransfer);
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleTransfer(@NotNull io.nats.client.Message m, @NotNull PlayerTransferConsumer.Message msg) {
        // todo why doesnt minestom have a get player by id for both states :dead:
        var players = new ArrayList<>(MinecraftServer.getConnectionManager().getOnlinePlayers());
        players.addAll(MinecraftServer.getConnectionManager().getConfigPlayers());
        var player = players.stream().filter(p -> p.getUuid().toString().equals(msg.playerId)).findFirst().orElse(null);
        if (player == null) return;

        try {
            if (msg.from != null) {
                var world = MapWorld.forPlayer(player);
                if (world == null) return;

                var fromHub = "hub".equals(msg.from);
                if (fromHub && !MapData.SPAWN_MAP_ID.equals(world.map().id()))
                    return;
                if (!fromHub && !msg.from.equals(world.map().id()))
                    return;
            }

            if ("hub".equals(msg.to) || MapData.SPAWN_MAP_ID.equals(msg.to)) {
                bridge.joinHub(player);
            } else {
                bridge.joinMap(player, msg.to, msg.state, "service_transfer");
            }
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
        }
    }

}
