package net.hollowcube.mapmaker.kafka;

import io.prometheus.client.Counter;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class FriendlyProducer implements AutoCloseable {
    private static final System.Logger logger = System.getLogger(FriendlyProducer.class.getName());

    private static final Counter producerMessagesSent = Counter.build()
            .name("kafka_producer_messages_sent")
            .help("Number of messages sent by the producer")
            .register();
    private static final Counter producerErrors = Counter.build()
            .name("kafka_producer_errors")
            .help("Number of errors encountered by the producer")
            .register();

    private final KafkaProducer<String, String> producer;

    public FriendlyProducer(@NotNull String bootstrapServers, boolean isNoop) {
        if (bootstrapServers.isEmpty() || isNoop) {
            producer = null;
            return;
        }

        //todo we should only have a single friendly producer per server probably.
        producer = new KafkaProducer<>(Map.of(
                "client.id", ServerRuntime.getRuntime().hostname() + "-" + ThreadLocalRandom.current().nextInt(1000),
                "bootstrap.servers", bootstrapServers,
                "batch.size", 0
        ), new StringSerializer(), new StringSerializer());
    }

    public @Blocking void produce(@NotNull String topic, @NotNull String value) {
        if (producer == null) return;

        var future = new CompletableFuture<>();
        producer.send(new ProducerRecord<>(topic, value), (unused1, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(null);
            }
        });
        FutureUtil.getUnchecked(future);
    }

    public void produceAndForget(@NotNull String topic, @NotNull String value) {
        if (producer == null) return;

        try {
            produce(topic, value);
        } catch (Exception e) {
            producerErrors.inc();
            logger.log(System.Logger.Level.ERROR, "Failed to produce message to topic " + topic, e);
        }
    }

    @Override
    public void close() {
        if (producer != null) producer.close();
    }
}
