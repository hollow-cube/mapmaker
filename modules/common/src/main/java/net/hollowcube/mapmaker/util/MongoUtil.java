package net.hollowcube.mapmaker.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.model.SaveState;
import net.minestom.server.coordinate.Pos;
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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MongoUtil {
    private MongoUtil() {}

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
            for (String mapId : value.getMapSlots()) {
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
                    case "spawn_point" -> {
                        reader.readStartDocument();
                        double x = 0.0;
                        double y = 0.0;
                        double z = 0.0;
                        float yaw = 0.0f;
                        float pitch = 0.0f;
                        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                            switch (reader.readName()) {
                                case "x" -> x = reader.readDouble();
                                case "y" -> y = reader.readDouble();
                                case "z" -> z = reader.readDouble();
                                case "yaw" -> yaw = (float) reader.readDouble();
                                case "pitch" -> pitch = (float) reader.readDouble();
                                default -> throw new RuntimeException("Unknown field: " + reader.readName());
                            }
                        }
                        reader.readEndDocument();
                        value.setSpawnPoint(new Pos(x, y, z, yaw, pitch));
                    }
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
                            long timestamp = reader.readInt64("timestamp");
                            reader.readEndDocument();
                            value.tryAddTime(id, timestamp);
                        }
                        reader.readEndArray();
                    }
                    case "published" -> value.setPublished(reader.readBoolean());
                    case "publishedId" -> value.setPublishedId(reader.readString());
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
            writer.writeStartDocument("spawn_point");
            writer.writeDouble("x", value.getSpawnPoint().x());
            writer.writeDouble("y", value.getSpawnPoint().y());
            writer.writeDouble("z", value.getSpawnPoint().z());
            writer.writeDouble("yaw", value.getSpawnPoint().yaw());
            writer.writeDouble("pitch", value.getSpawnPoint().pitch());
            writer.writeEndDocument();
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
                writer.writeInt64("timestamp", completion.timeInMills());
                writer.writeEndDocument();
            }
            writer.writeEndArray();
            if (value.isPublished()) {
                writer.writeBoolean("published", true);
                writer.writeString("publishedId", value.getPublishedId());
            }

            writer.writeEndDocument();
        }

        @Override
        public Class<MapData> getEncoderClass() {
            return MapData.class;
        }
    };

    private static final Codec<SaveState> SAVE_STATE_CODEC = new Codec<>() {
        @Override
        public SaveState decode(BsonReader reader, DecoderContext decoderContext) {
            var value = new SaveState();
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                switch (reader.readName()) {
                    case "_id" -> value.setId(reader.readString());
                    case "player_id" -> value.setPlayerId(reader.readString());
                    case "map_id" -> value.setMapId(reader.readString());
                    case "completed" -> value.setCompleted(reader.readBoolean());
                    case "start_time" -> value.setStartTime(Instant.ofEpochMilli(reader.readDateTime()));
                    case "playtime" -> value.setPlaytime(reader.readInt64());
                    case "pos" -> {
                        reader.readStartDocument();
                        double x = 0.0;
                        double y = 0.0;
                        double z = 0.0;
                        float yaw = 0.0f;
                        float pitch = 0.0f;
                        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                            switch (reader.readName()) {
                                case "x" -> x = reader.readDouble();
                                case "y" -> y = reader.readDouble();
                                case "z" -> z = reader.readDouble();
                                case "yaw" -> yaw = (float) reader.readDouble();
                                case "pitch" -> pitch = (float) reader.readDouble();
                                default -> throw new RuntimeException("Unknown field: " + reader.readName());
                            }
                        }
                        reader.readEndDocument();
                        value.setPos(new Pos(x, y, z, yaw, pitch));
                    }
                }
            }
            reader.readEndDocument();
            return value;
        }

        @Override
        public void encode(BsonWriter writer, SaveState value, EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeString("_id", value.getId());
            writer.writeString("player_id", value.getPlayerId());
            writer.writeString("map_id", value.getMapId());
            if (value.isCompleted())
                writer.writeBoolean("completed", true);
            writer.writeDateTime("start_time", value.getStartTime().toEpochMilli());
            writer.writeInt64("playtime", value.getPlaytime());
            writer.writeStartDocument("pos");
            writer.writeDouble("x", value.getPos().x());
            writer.writeDouble("y", value.getPos().y());
            writer.writeDouble("z", value.getPos().z());
            writer.writeDouble("yaw", value.getPos().yaw());
            writer.writeDouble("pitch", value.getPos().pitch());
            writer.writeEndDocument();
            writer.writeEndDocument();
        }

        @Override
        public Class<SaveState> getEncoderClass() {
            return SaveState.class;
        }
    };

    private static final Map<String, MongoClient> clients = new ConcurrentHashMap<>();

    public static final MongoClientSettings BASE_CLIENT_SETTINGS = MongoClientSettings.builder()
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(CodecRegistries.fromRegistries(
                    CodecRegistries.fromCodecs(PLAYER_DATA_CODEC, MAP_DATA_CODEC, SAVE_STATE_CODEC),
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
