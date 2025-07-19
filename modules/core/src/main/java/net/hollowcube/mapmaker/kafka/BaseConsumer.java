package net.hollowcube.mapmaker.kafka;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.ExceptionReporter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class BaseConsumer<T> implements AutoCloseable {

    private final KafkaConsumer<String, String> consumer;
    private final Function<String, T> valueDeserializer;

    private boolean autocommit = true;

    protected BaseConsumer(@NotNull String topic, @NotNull String groupId,
                           @NotNull Function<String, T> deserializer,
                           @NotNull String bootstrapServers) {
        if (bootstrapServers.isEmpty()) {
            consumer = null;
            valueDeserializer = null;
            return;
        }

        //todo we should not be creating a distinct kafka consumer for each one of these. We only need to create one consumer and simply subscribe it to many topics.

        this.valueDeserializer = deserializer;

        var runtime = ServerRuntime.getRuntime();
        consumer = new KafkaConsumer<>(Map.of(
                "client.id", runtime.hostname() + "-" + topic,
                "group.id", runtime.hostname(),
                "bootstrap.servers", bootstrapServers,
                "max.poll.interval.ms", "900000"
        ), new StringDeserializer(), new StringDeserializer());
        consumer.subscribe(List.of(topic));

        new Thread(this::pollLoop).start();
    }

    protected void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }

    protected abstract void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull T message);

    void pollLoop() {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                var records = consumer.poll(Duration.ofMillis(50));
                for (var kafkaRecord : records) {
                    var value = valueDeserializer.apply(kafkaRecord.value());
                    if (value != null) {
                        onMessage(kafkaRecord, value);
                    }
                }

                if (!records.isEmpty() && autocommit) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            // Intentionally do nothing, this is the exit condition.
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
        } finally {
            consumer.close();
        }
    }

    @Override
    public void close() {
        if (consumer != null) {
            consumer.wakeup();
        }
    }
}
