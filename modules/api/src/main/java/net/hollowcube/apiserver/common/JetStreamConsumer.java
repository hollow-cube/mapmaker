package net.hollowcube.apiserver.common;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.MessageConsumer;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StreamConfiguration;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

/// A durable consumer on a stream it creates, or updates, before subscribing. Every api-server
/// starts the same one, so each message is handled by one of them.
///
/// A message is acked once [#handle] returns and delivered again if it throws.
public abstract class JetStreamConsumer implements AutoCloseable {

    private static final int CLUSTER_REPLICAS = 3;
    private static final Duration RETRY = Duration.ofSeconds(5);
    private static final int STREAM_EXISTS_DIFFERENTLY = 10058;

    private static final Logger logger = LoggerFactory.getLogger(JetStreamConsumer.class);

    private final Connection nc;
    private final StreamConfiguration.Builder stream;
    private final ConsumerConfiguration consumer;
    private volatile boolean closed;
    private volatile @Nullable MessageConsumer running;

    /// @param stream everything but the replicas, which follow the broker
    protected JetStreamConsumer(
        NatsPublisher nats,
        StreamConfiguration.Builder stream,
        ConsumerConfiguration consumer
    ) {
        this.nc = nats.connection();
        this.stream = stream;
        this.consumer = consumer;
    }

    protected abstract void handle(Message message);

    /// Returns at once; the broker may not be reachable yet, and nothing else waits on this.
    public void start() {
        Thread.ofVirtual().name(consumer.getDurable()).start(this::subscribe);
    }

    private void subscribe() {
        while (!closed) {
            try {
                var name = ensureStream();
                var started = nc.getStreamContext(name)
                    .createOrUpdateConsumer(consumer)
                    .consume(this::deliver);
                running = started;
                // A close that landed while this was subscribing found nothing to stop.
                if (closed) started.stop();
                logger.info("consuming {} as {}", name, consumer.getDurable());
                return;
            } catch (IOException | JetStreamApiException | RuntimeException e) {
                logger.error(
                    "could not start consuming as {}, trying again",
                    consumer.getDurable(),
                    e
                );
            }
            try {
                Thread.sleep(RETRY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private String ensureStream() throws IOException, JetStreamApiException {
        // A broker that is not part of a cluster refuses a stream with more than one replica.
        var clustered = nc.getServerInfo().getCluster() != null;
        var config = stream.replicas(clustered ? CLUSTER_REPLICAS : 1).build();
        logger.info("stream {} on {} replica(s)", config.getName(), config.getReplicas());
        var management = nc.jetStreamManagement();
        try {
            management.addStream(config);
        } catch (JetStreamApiException e) {
            if (e.getApiErrorCode() != STREAM_EXISTS_DIFFERENTLY) throw e;
            management.updateStream(config);
        }
        return config.getName();
    }

    private void deliver(Message message) {
        try {
            handle(message);
            message.ack();
        } catch (RuntimeException e) {
            logger.error(
                "could not handle a message on {}, it will be delivered again",
                message.getSubject(),
                e
            );
            message.nakWithDelay(RETRY);
        }
    }

    protected static @Nullable String header(Message message, String name) {
        var headers = message.getHeaders();
        return headers == null ? null : headers.getFirst(name);
    }

    /// Throws [IllegalArgumentException] naming the header when it is missing or empty.
    protected static String requiredHeader(Message message, String name) {
        var value = header(message, name);
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("no " + name);
        return value;
    }

    protected static long longHeader(Message message, String name) {
        try {
            return Long.parseLong(requiredHeader(message, name));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " is not a number");
        }
    }

    @Override
    public void close() {
        closed = true;
        var running = this.running;
        // jnats throws on a connection that is already closed, which took the consumer with it.
        if (running != null && nc.getStatus() != Connection.Status.CLOSED) running.stop();
    }
}
