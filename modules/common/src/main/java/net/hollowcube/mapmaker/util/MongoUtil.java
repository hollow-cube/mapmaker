package net.hollowcube.mapmaker.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MongoUtil {
    private MongoUtil() {
    }

    private static final Codec<PlayerData> PLAYER_DATA_CODEC = new Codec<>() {
        @Override
        public PlayerData decode(BsonReader reader, DecoderContext decoderContext) {
            var player = new PlayerData();
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                switch (reader.readName()) {
                    case "_id" -> player.setId(reader.readString());
                    case "uuid" -> player.setUuid(reader.readString());
                }
            }
            reader.readEndDocument();
            return player;
        }

        @Override
        public void encode(BsonWriter writer, PlayerData value, EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeString("_id", value.getId());
            writer.writeString("uuid", value.getUuid());
            writer.writeEndDocument();
        }

        @Override
        public Class<PlayerData> getEncoderClass() {
            return PlayerData.class;
        }
    };

    private static final Codec<MapData> MAP_DATA_CODEC = new Codec<>() {
        @Override
        public MapData decode(BsonReader reader, DecoderContext decoderContext) {
            var value = new MapData();
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                switch (reader.readName()) {
                    case "_id" -> value.setId(reader.readString());
                    case "type" -> value.setType(MapData.Type.valueOf(reader.readString()));
                    case "name" -> value.setName(reader.readString());
                    case "mapFileId" -> value.setMapFileId(reader.readString());
                }
            }
            reader.readEndDocument();
            return value;
        }

        @Override
        public void encode(BsonWriter writer, MapData value, EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeString("_id", value.getId());
            writer.writeString("type", value.getType().name());
            writer.writeString("name", value.getName());
            if (value.getMapFileId() != null) {
                writer.writeString("mapFileId", value.getMapFileId());
            }
            writer.writeEndDocument();
        }

        @Override
        public Class<MapData> getEncoderClass() {
            return MapData.class;
        }
    };

    private static final Codec<Metric> METRIC_CODEC = new Codec<>() {
        @Override
        public void encode(BsonWriter writer, Metric value, EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeInt32("id", value.getId()); // NOT the primary key
            writer.writeString("source", value.getSource());
            writer.writeString("target", value.getTarget());
            writer.writeDouble("value", value.getValue());
            writer.writeEndDocument();
        }

        @Override
        public Class<Metric> getEncoderClass() {
            return Metric.class;
        }

        @Override
        public Metric decode(BsonReader reader, DecoderContext decoderContext) {
            var metric = new Metric();
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                switch (reader.readName()) {
                    case "id" -> metric.setId(reader.readInt32());
                    case "source" -> metric.setSource(reader.readString());
                    case "target" -> metric.setTarget(reader.readString());
                    case "value" -> metric.setValue(reader.readDouble());
                }
            }
            reader.readEndDocument();
            return metric;
        }
    };

    private static final Map<String, MongoClient> clients = new ConcurrentHashMap<>();

    public static final MongoClientSettings BASE_CLIENT_SETTINGS = MongoClientSettings.builder()
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(CodecRegistries.fromRegistries(
                    CodecRegistries.fromCodecs(PLAYER_DATA_CODEC, MAP_DATA_CODEC),
                    MongoClientSettings.getDefaultCodecRegistry()
            ))
            .build();
    public static final CodecRegistry DEFAULT_CODEC_REGISTRY = CodecRegistries.withUuidRepresentation(
            BASE_CLIENT_SETTINGS.getCodecRegistry(), BASE_CLIENT_SETTINGS.getUuidRepresentation());

    public static @NotNull MongoClient getClient(@NotNull String uri) {
        return clients.computeIfAbsent(uri, unused ->
                MongoClients.create(MongoClientSettings.builder(BASE_CLIENT_SETTINGS)
                        .applyConnectionString(new ConnectionString(uri))
                        .build())
        );
    }

}
