package net.hollowcube.mapmaker.storage;

import com.google.auto.service.AutoService;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.metrics.Metric;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

class MetricStorageMongo implements MetricStorage {
    private final MongoClient client;
    private final MongoConfig config;

    public MetricStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull Metric addMetric(@NotNull Metric metric) {
        try {
            collection().insertOne(metric);
        } catch (DuplicateKeyException ignored) {

        } catch (Exception e) {
            e.printStackTrace();
        }
        return metric;
    }

    private @NotNull MongoCollection<Metric> collection() {
        return client.getDatabase(config.database()).getCollection("metrics", Metric.class);
    }

    @AutoService(Codec.class)
    public static final class MetricCodec implements Codec<Metric> {
        @Override
        public Metric decode(BsonReader reader, DecoderContext decoderContext) {
            var metric = new Metric<>();
            // TODO not needed yet since we shouldn't be retrieving metrics from mongo in code
            return metric;
        }

        @Override
        public void encode(BsonWriter writer, Metric value, EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeString("tag", value.getTag());
            writer.writeInt64("timestamp", value.getTimestamp());
            writer.writeStartArray("values");
            for (Object o : value.getValues().entrySet()) {
                var hashValue = (Map.Entry) o;
                var valueVal = hashValue.getKey();
                var valueType = hashValue.getValue();
                switch ((Metric.ValueType) valueType) {
                    case DOUBLE -> writer.writeDouble((Double) valueVal);
                    case STRING -> writer.writeString((String) valueVal);
                    case BOOLEAN -> writer.writeBoolean((Boolean) valueVal);
                    case INT32 -> writer.writeInt32((Integer) valueVal);
                    case INT64 -> writer.writeInt64((Long) valueVal);
                }
            }
            writer.writeEndArray();
            writer.writeEndDocument();
        }

        @Override
        public Class<Metric> getEncoderClass() {
            return Metric.class;
        }
    }
}
