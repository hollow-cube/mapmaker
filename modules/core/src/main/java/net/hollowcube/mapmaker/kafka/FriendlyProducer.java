package net.hollowcube.mapmaker.kafka;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.prometheus.client.Counter;
import net.hollowcube.common.ServerRuntime;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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

    public FriendlyProducer(@NotNull String bootstrapServers) {
        producer = new KafkaProducer<>(Map.of(
                "client.id", ServerRuntime.getRuntime().hostname(),
                "bootstrap.servers", bootstrapServers
        ), new StringSerializer(), new StringSerializer());
    }

    public @NotNull ListenableFuture<Void> produce(@NotNull String topic, @NotNull String value) {
        var future = SettableFuture.<Void>create();
        producer.send(new ProducerRecord<>(topic, value), (unused1, exception) -> {
            if (exception != null) {
                future.setException(exception);
            } else {
                future.set(null);
            }
        });
        return future;
    }

    public void produceAndForget(@NotNull String topic, @NotNull String value) {
        Futures.addCallback(produce(topic, value), new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                producerMessagesSent.inc();
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                producerErrors.inc();
                logger.log(System.Logger.Level.ERROR, "Failed to produce message to topic " + topic, t);
            }
        }, Runnable::run);
    }

    @Override
    public void close() {
        producer.close();
    }
}
