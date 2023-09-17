package net.hollowcube.mapmaker.map;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MapMgmtConsumer extends BaseConsumer<MapMgmtConsumer.MapUpdateMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MapMgmtConsumer.class);

    public record MapUpdateMessage(
            @MagicConstant(valuesFromClass = MapUpdateMessage.class) int action,
            @NotNull String id
    ) {
        public static final int ACTION_CREATE = 0;
        public static final int ACTION_DELETE = 1;

        public static @NotNull MapUpdateMessage fromJson(@NotNull String json) {
            return AbstractHttpService.GSON.fromJson(json, MapUpdateMessage.class);
        }
    }

    private static final String TOPIC_NAME = "map_mgmt";
    private static final String GROUP_ID = ServerRuntime.getRuntime().hostname();

    public MapMgmtConsumer(@NotNull String bootstrapServers) {
        super(TOPIC_NAME, GROUP_ID, MapUpdateMessage::fromJson, bootstrapServers);
        setAutocommit(false);
    }

    @Override
    protected final void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull MapUpdateMessage msg) {
        logger.info("Received map update message ({}): {}", msg.action, msg);
        switch (msg.action) {
//            case MapUpdateMessage.ACTION_CREATE -> handleMapCreate(msg.data.id());
            case MapUpdateMessage.ACTION_DELETE -> handleMapDelete(msg.id());
        }
    }

    protected void handleMapDelete(@NotNull String mapId) {
    }
}
