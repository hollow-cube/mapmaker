package net.hollowcube.mapmaker.kafka;

import net.hollowcube.common.ServerRuntime;
import net.minestom.server.MinecraftServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

public abstract class BaseConsumer<T> implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final KafkaConsumer<String, String> consumer;
    private final Function<String, T> valueDeserializer;

    private final ScheduledFuture<?> handle;

    private boolean autocommit = true;

    // KafkaConsumer is not thread safe, so if this is set (from another thread), then the consumer thread
    // will see it, close, and then complete it allowing shutdown to finalize.
    private volatile CompletableFuture<Void> closeFuture = null;

    protected BaseConsumer(@NotNull String topic,
                           @NotNull Function<String, T> deserializer,
                           @NotNull String bootstrapServers) {
        if (bootstrapServers.isEmpty()) {
            consumer = null;
            valueDeserializer = null;
            handle = null;
            return;
        }

        //todo we should not be creating a distinct kafka consumer for each one of these. We only need to create one consumer and simply subscribe it to many topics.

        this.valueDeserializer = deserializer;

        var runtime = ServerRuntime.getRuntime();
        consumer = new KafkaConsumer<>(Map.of(
                "client.id", runtime.hostname() + "-" + topic,
                "group.id", runtime.hostname(),
                "bootstrap.servers", bootstrapServers
        ), new StringDeserializer(), new StringDeserializer());
        consumer.subscribe(List.of(topic));

        handle = executor.scheduleAtFixedRate(this::poll, 0, 50, TimeUnit.MILLISECONDS);
    }

    protected void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }

    protected abstract void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull T message);

    void poll() {
        if (closeFuture != null) {
            consumer.close();
            closeFuture.complete(null);
            return;
        }
        
        try {
            var records = consumer.poll(Duration.ofMillis(50));
            for (var kafkaRecord : records) {
                var value = valueDeserializer.apply(kafkaRecord.value());
                if (value != null) {
                    onMessage(kafkaRecord, value);
                }
            }

            if (autocommit) {
                consumer.commitSync();
            }
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    @Override
    public void close() {
        if (consumer != null) {
            closeFuture = new CompletableFuture<>();
            closeFuture.join();
        }
        if (handle != null) handle.cancel(false);
    }
}
