package net.hollowcube.mapmaker.map;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class MapPlayerDataMgmtConsumer extends BaseConsumer<MapPlayerDataMgmtConsumer.MapPlayerDataUpdateMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MapPlayerDataMgmtConsumer.class);

    public record MapPlayerDataUpdateMessage(
            @MagicConstant(valuesFromClass = MapPlayerDataUpdateMessage.class) int action,
            @NotNull MapPlayerData data
    ) {
        public static final int ACTION_UPDATE = 0;

        public static @NotNull MapPlayerDataUpdateMessage fromJson(@NotNull String json) {
            return AbstractHttpService.GSON.fromJson(json, MapPlayerDataUpdateMessage.class);
        }
    }

    private static final String TOPIC_NAME = "map_player_data_mgmt";
    private static final String GROUP_ID = ServerRuntime.getRuntime().hostname();

    public MapPlayerDataMgmtConsumer(@NotNull String bootstrapServers) {
        super(TOPIC_NAME, GROUP_ID, MapPlayerDataUpdateMessage::fromJson, bootstrapServers);
        setAutocommit(false);
    }

    @Override
    protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull MapPlayerDataUpdateMessage msg) {
        logger.info("Received map player data update message ({}): {}", msg.action, msg);
        if (msg.action != MapPlayerDataUpdateMessage.ACTION_UPDATE) return;

        var data = msg.data;
        var player = MinecraftServer.getConnectionManager().getPlayer(UUID.fromString(data.id()));
        if (player == null) return;

        MapPlayerData.fromPlayer(player).update(data);
    }
}
