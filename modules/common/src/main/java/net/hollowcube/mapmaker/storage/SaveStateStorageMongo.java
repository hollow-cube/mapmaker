package net.hollowcube.mapmaker.storage;

import com.google.auto.service.AutoService;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.model.SaveState;
import net.minestom.server.coordinate.Pos;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.descending;

public class SaveStateStorageMongo implements SaveStateStorage {
    private static final String DB_NAME = System.getProperty("mongo.db", "mapmaker");

    private final MongoClient client;

    public SaveStateStorageMongo(@NotNull MongoClient client) {
        this.client = client;
    }

    @Override
    public @NotNull FutureResult<@NotNull SaveState> createSaveState(@NotNull SaveState saveState) {
        return FutureResult.supply(() -> {
            try {
                collection().insertOne(saveState);
            } catch (DuplicateKeyException ignored) {
                return Result.error(ERR_DUPLICATE_ENTRY);
            }
            return Result.of(saveState);
        });
    }

    @Override
    public @NotNull FutureResult<Void> updateSaveState(@NotNull SaveState saveState) {
        return FutureResult.supply(() -> {
            var filter = eq("_id", saveState.getId());
            var result = collection().replaceOne(filter, saveState);
            if (result.getModifiedCount() == 0)
                return Result.error(ERR_NOT_FOUND);
            return Result.ofNull();
        });
    }

    @Override
    public @NotNull FutureResult<@NotNull SaveState> getLatestSaveState(@NotNull String playerId, @NotNull String mapId) {
        return FutureResult.supply(() -> {
            var filter = and(
                    eq("player_id", playerId),
                    eq("map_id", mapId),
                    or(eq("complete", false), not(exists("complete"))));
            var sort = descending("start_time");
            var result = collection().find(filter, SaveState.class).sort(sort).limit(1).first();
            if (result == null)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(result);
        });
    }

    private @NotNull MongoCollection<SaveState> collection() {
        return client.getDatabase(DB_NAME).getCollection("savestates", SaveState.class);
    }

    @AutoService(Codec.class)
    public static final class SaveStateCodec implements Codec<SaveState> {
        public static final SaveStateCodec INSTANCE = new SaveStateCodec();

        private SaveStateCodec() {}

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
    }
}
