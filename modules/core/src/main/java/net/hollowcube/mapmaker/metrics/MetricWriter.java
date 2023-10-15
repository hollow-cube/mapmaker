package net.hollowcube.mapmaker.metrics;

import net.hollowcube.common.ServerRuntime;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MetricWriter {
    private static final System.Logger logger = System.getLogger(MetricWriter.class.getName());

    private static KafkaProducer<String, Metric> producer;
    private static final String TOPIC = "metrics";

    public MetricWriter(@NotNull String bootstrapServers) {
        if (bootstrapServers.isEmpty()) {
            producer = null;
            return;
        }

        producer = new KafkaProducer<>(Map.of(
                "client.id", ServerRuntime.getRuntime().hostname(),
                "bootstrap.servers", bootstrapServers
        ), new StringSerializer(), new MetricSerializer());
    }

    public @Blocking void writeMetric(@NotNull Metric metric) {
        if (producer == null) {
            logger.log(System.Logger.Level.ERROR, "metric kafka producer is null");
        }

        var future = new CompletableFuture<>();
        producer.send(new ProducerRecord<>(TOPIC, metric), (unused, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                logger.log(System.Logger.Level.INFO, "Wrote metric " + metric.toString());
                future.complete(null);
            }
        });
    }
}
