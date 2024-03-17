package net.hollowcube.mapmaker.metrics;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import net.hollowcube.common.util.FutureUtil;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificDatumWriter;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricWriterImpl implements MetricWriter {
    private static final Logger logger = LoggerFactory.getLogger(MetricWriterImpl.class);

    private static final String SCHEMA_REGISTRY_URL = "http://schema-registry.metrics.hollowcube.dev:30212";
    private static final String KAFKA_PROXY_URL = "proxy.metrics.hollowcube.dev:30212";
    private static final String AUTH_USERNAME = "AtjaJHdN2bh6";

    private final ReflectData avroReflectData;
    private final SchemaRegistryClient schemaRegistryClient;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String authHeader;

    private final Map<Class<?>, WrappedSchema> schemas = new ConcurrentHashMap<>();

    public MetricWriterImpl(@NotNull String password) {
        avroReflectData = ReflectData.get();
        avroReflectData.addLogicalTypeConversion(new InstantConversion());

        var usernamePassword = String.format("%s:%s", AUTH_USERNAME, password);
        authHeader = String.format("Basic %s", Base64.getEncoder().encodeToString(usernamePassword.getBytes(StandardCharsets.UTF_8)));

        this.schemaRegistryClient = new CachedSchemaRegistryClient(SCHEMA_REGISTRY_URL, 20, Map.of(), Map.of(
                "Authorization", authHeader
        ));
    }

    @Override
    public void write(@NotNull Metric metric) {
        FutureUtil.submitVirtual(() -> {
            try {
                writeInternal(metric); // Blocking call
            } catch (Exception e) {
                logger.warn("Failed to write metric", e);
            }
        });
    }

    @Blocking
    private void writeInternal(@NotNull Metric metric) throws Exception {
        var wrappedSchema = schemas.computeIfAbsent(metric.getClass(), clazz -> new WrappedSchema(avroReflectData, clazz));

        var schemaSubject = String.format("%s-value", wrappedSchema.topicName());
        var schemaId = schemaRegistryClient.register(schemaSubject, wrappedSchema.schema());

        var rawPayload = wrappedSchema.write(metric);
        var buffer = ByteBuffer.allocate(5 + rawPayload.length);
        buffer.put((byte) 0); // Magic number
        buffer.putInt(schemaId);
        buffer.put(rawPayload);
        var payload = buffer.array();

        var uri = URI.create(String.format("http://%s/%s", KAFKA_PROXY_URL, wrappedSchema.topicName()));
        var req = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .header("Authorization", authHeader)
                .build();
        var res = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        if (res.statusCode() != 200) throw new RuntimeException("Failed to write metric: " + res.statusCode());
    }

    public static class WrappedSchema {
        private final String topicName;
        private final AvroSchema schema;
        private final SpecificDatumWriter<Metric> writer;
        private BinaryEncoder encoder = null;

        public WrappedSchema(@NotNull ReflectData reflectData, @NotNull Class<?> clazz) {
            this.topicName = computeTopicName(clazz.getSimpleName());
            this.schema = new AvroSchema(reflectData.getSchema(clazz));
            this.writer = new SpecificDatumWriter<>(schema.rawSchema(), reflectData);
        }

        public @NotNull String topicName() {
            return topicName;
        }

        public @NotNull AvroSchema schema() {
            return schema;
        }

        public byte[] write(@NotNull Metric metric) {
            var out = new ByteArrayOutputStream();
            encoder = EncoderFactory.get().binaryEncoder(out, this.encoder);
            try {
                writer.write(metric, encoder);
                encoder.flush();
                return out.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static @NotNull String computeTopicName(@NotNull String className) {
        return "metric_" + className.replace("Event", "")
                .replaceAll("([a-z])([A-Z]+)", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }

}
