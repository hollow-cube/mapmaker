package net.hollowcube.mapmaker.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.coordinate.Vec;
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
import java.util.UUID;
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
                    case "unlocked_map_slots" -> player.setUnlockedMapSlots(reader.readInt32());
                    case "map_slots" -> {
                        reader.readStartArray();
                        for (int i = 0; i < PlayerData.MAX_MAP_SLOTS; i++) {
                            if (reader.readBsonType() == BsonType.NULL) {
                                reader.readNull();
                                continue;
                            }
                            player.setMapSlot(i, reader.readString());
                        }
                        reader.readEndArray();
                    }
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
            writer.writeInt32("unlocked_map_slots", value.getUnlockedMapSlots());
            writer.writeStartArray("map_slots");
            for (var mapId : value.getMapSlots()) {
                if (mapId == null) {
                    writer.writeNull();
                } else {
                    writer.writeString(mapId);
                }
            }
            writer.writeEndArray();
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
                    case "owner" -> value.setOwner(reader.readString());
                    case "name" -> value.setName(reader.readString());
                    case "mapFileId" -> value.setMapFileId(reader.readString());
                    case "pois" -> {
                        reader.readStartArray();
                        while (reader.readBsonType() == BsonType.DOCUMENT) {
                            reader.readStartDocument();
                            var type = reader.readString("type");
                            reader.readName();
                            reader.readStartDocument();
                            var pos = new Vec(
                                    reader.readDouble("x"),
                                    reader.readDouble("y"),
                                    reader.readDouble("z"));
                            reader.readEndDocument();

                            value.addPOI(new MapData.POI(type, pos));
                            reader.readEndDocument();
                        }
                        reader.readEndArray();
                    }
                    case "completion-times" -> {
                        reader.readStartArray();
                        while (reader.readBsonType() == BsonType.DOCUMENT) {
                            reader.readStartDocument();
                            UUID id = UUID.fromString(reader.readString("uuid"));
                            long timestamp = reader.readInt64();
                            reader.readEndDocument();
                            value.tryAddTime(id, timestamp);
                        }
                        reader.readEndArray();
                    }
                }
            }
            reader.readEndDocument();
            return value;
        }

        @Override
        public void encode(BsonWriter writer, MapData value, EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeString("_id", value.getId());
            writer.writeString("owner", value.getOwner());
            writer.writeString("name", value.getName());
            if (value.getMapFileId() != null) {
                writer.writeString("mapFileId", value.getMapFileId());
            }
            writer.writeStartArray("pois");
            for (var poi : value.getPois()) {
                writer.writeStartDocument();
                writer.writeString("type", poi.type());
                writer.writeStartDocument("pos");
                writer.writeDouble("x", poi.pos().x());
                writer.writeDouble("y", poi.pos().y());
                writer.writeDouble("z", poi.pos().z());
                writer.writeEndDocument();
                writer.writeEndDocument();
            }
            writer.writeEndArray();
            writer.writeStartArray("completion-times");
            for (var completion : value.getCompletionTimes()) {
                writer.writeStartDocument();
                writer.writeString("uuid", completion.playerUUID().toString());
                writer.writeInt64(completion.timeInMills());
                writer.writeEndDocument();
            }
            writer.writeEndArray();
            writer.writeEndDocument();
        }

        @Override
        public Class<MapData> getEncoderClass() {
            return MapData.class;
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
