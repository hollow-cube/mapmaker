package net.hollowcube.mapmaker.scripting;

import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/// Per-editing-world NATS change source.
///
/// The editor pushes file updates to the backend, which publishes one message
/// per changed file to {@code map-files.{mapId}}. We run an ephemeral consumer
/// filtered to just this map; it is auto-cleaned by the {@code inactiveThreshold}
/// and explicitly torn down on world close.
///
/// Each message is handed to [ScriptContext#notifyFilesChanged] on a virtual
/// thread (the actual fetch is blocking, and the reload itself is debounced and
/// confined to the world scheduler by [ScriptContext]).
public final class NatsChangeSource implements ScriptChangeSource {
    private static final Logger logger = LoggerFactory.getLogger(NatsChangeSource.class);

    public static final String STREAM_NAME = "MAP_FILES";

    @RuntimeGson
    public record MapFileMessage(String mapId, String path) {
    }

    private final JetStreamWrapper jetStream;
    private final String mapId;
    private final ScriptContext scripts;

    private @Nullable MessageConsumer consumer;

    public NatsChangeSource(JetStreamWrapper jetStream, String mapId, ScriptContext scripts) {
        this.jetStream = jetStream;
        this.mapId = mapId;
        this.scripts = scripts;
    }

    @Override
    public void start() {
        if (consumer != null) throw new IllegalStateException("already started");

        // Ephemeral, scoped to this one map. Only deliver changes from now on -
        // the world bootstraps the full file set separately on startup.
        var config = ConsumerConfiguration.builder()
            .filterSubjects("map-files." + mapId)
            .deliverPolicy(DeliverPolicy.New)
            .ackPolicy(AckPolicy.None)
            .inactiveThreshold(Duration.ofMinutes(5))
            .build();

        this.consumer = jetStream.subscribe(STREAM_NAME, config, MapFileMessage.class, this::handle);
        logger.info("[scripts:{}] subscribed to map-files.{}", mapId, mapId);
    }

    private void handle(Message m, MapFileMessage msg) {
        // Sanity: our subject filter should limit to only this map anyway.
        if (!mapId.equals(msg.mapId())) return;

        logger.info("[scripts:{}] file changed: {}", mapId, msg.path());
        // Off the NATS dispatcher thread: notifyFilesChanged does blocking IO,
        // then hands a debounced, world-thread-confined reload to ScriptContext.
        FutureUtil.submitVirtual(() -> scripts.notifyFilesChanged(List.of(msg.path())));
    }

    @Override
    public void close() {
        if (consumer == null) return;
        try {
            consumer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            consumer = null;
        }
    }
}
