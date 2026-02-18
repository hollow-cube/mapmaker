package net.hollowcube.mapmaker.map;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.time.ZonedDateTime;

public abstract class MapMgmtConsumer implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(MapMgmtConsumer.class);

    @RuntimeGson
    public record MapUpdateMessage(
        @MagicConstant(valuesFromClass = MapUpdateMessage.class) int action,
        @NotNull String id,
        @Nullable String drainReason // Only present for drain, not required.
    ) {
        public static final int ACTION_CREATE = 0;
        public static final int ACTION_DELETE = 1;
        public static final int ACTION_DRAIN = 2;

        public @NotNull String subject() {
            return "maps." + switch (action) {
                case ACTION_CREATE -> "create";
                case ACTION_DELETE -> "delete";
                case ACTION_DRAIN -> "drain";
                default -> throw new IllegalStateException("Unknown action: " + action);
            };
        }
    }

    public static final String STREAM_NAME = "MAP_MANAGEMENT";
    private static final ConsumerConfiguration CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("maps.>")
        // Process anything 1m prior in case we are starting as the map is drained/deleted
        .deliverPolicy(DeliverPolicy.ByStartTime)
        .startTime(ZonedDateTime.now().minus(Duration.ofMinutes(1)))
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    private final MessageConsumer consumer;

    public MapMgmtConsumer(@NotNull JetStreamWrapper jetStream) {
        this.consumer = jetStream.subscribe(STREAM_NAME, CONSUMER_CONFIG, MapUpdateMessage.class, this::handleMapUpdate);
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMapUpdate(@NotNull Message m, @NotNull MapUpdateMessage msg) {
        logger.info("Received map update message ({}): {}", msg.action, msg);
        FutureUtil.submitVirtual(() -> {
            switch (msg.action) {
                case MapUpdateMessage.ACTION_CREATE -> handleMapCreate(msg.id());
                case MapUpdateMessage.ACTION_DELETE -> handleMapDelete(msg.id());
                case MapUpdateMessage.ACTION_DRAIN -> handleMapDrain(msg.id(), msg.drainReason());
            }
        });
    }

    protected void handleMapCreate(@NotNull String mapId) {
    }

    protected void handleMapDelete(@NotNull String mapId) {
    }

    protected void handleMapDrain(@NotNull String mapId, @Nullable String reason) {
    }

}
