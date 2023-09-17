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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class BaseConsumer<T> implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final KafkaConsumer<String, String> consumer;
    private final Function<String, T> valueDeserializer;

    private final ScheduledFuture<?> handle;

    private boolean autocommit = true;

    protected BaseConsumer(@NotNull String topic, @NotNull String groupId,
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

        handle = executor.scheduleAtFixedRate(this::poll, 0, 500, TimeUnit.MILLISECONDS);
    }

    protected void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }

    protected abstract void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull T message);

    void poll() {
        try {
            var records = consumer.poll(Duration.ofMillis(100));
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
        if (handle != null) handle.cancel(false);
        if (consumer != null) consumer.close();
    }
}
