package net.hollowcube.mapmaker.storage;

import com.google.auto.service.AutoService;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.model.PlayerData;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

class PlayerStorageMongo implements PlayerStorage {
    private final MongoClient client;
    private final MongoConfig config;

    public PlayerStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull FutureResult<@NotNull PlayerData> createPlayer(@NotNull PlayerData player) {
        return FutureResult.supply(() -> {
            try {
                collection().insertOne(player);
            } catch (DuplicateKeyException ignored) {
                return Result.error(ERR_DUPLICATE_ENTRY);
            }
            return Result.of(player);
        });
    }

    @Override
    public @NotNull FutureResult<@NotNull PlayerData> getPlayerById(@NotNull String id) {
        return FutureResult.supply(() -> {
            var filter = eq("_id", id);
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(result);
        });
    }

    @Override
    public @NotNull FutureResult<@NotNull PlayerData> getPlayerByUuid(@NotNull String uuid) {
        return FutureResult.supply(() -> {
            var filter = eq("uuid", uuid);
            var result = collection().find(filter).limit(1).first();
            if (result == null)
                return Result.error(ERR_NOT_FOUND);
            return Result.of(result);
        });
    }

    @Override
    public @NotNull FutureResult<@NotNull Void> updatePlayer(@NotNull PlayerData player) {
        return FutureResult.supply(() -> {
            var filter = eq("_id", player.getId());
            var result = collection().replaceOne(filter, player);
            if (result.getModifiedCount() == 0)
                return Result.error(ERR_NOT_FOUND);
            return Result.ofNull();
        });
    }

    @Override
    public @NotNull FutureResult<Void> unlinkMap(@NotNull String mapId) {
        List<FutureResult<Void>> futures = new ArrayList<>();
        return FutureResult.supply(() -> {
            var filter = in("map_slots", mapId);
            collection().find(filter).forEach(player -> {
                for (int i = 0; i < player.getUnlockedMapSlots(); i++) {
                    if (player.getMapSlot(i).equals(mapId)) {
                        player.setMapSlot(i, null);
                    }
                }

                //todo update as a transaction
                futures.add(updatePlayer(player));
            });
            return Result.ofNull();
        }).flatMap(unused -> FutureResult.allOf(futures.toArray(FutureResult[]::new)));
    }

    private @NotNull MongoCollection<PlayerData> collection() {
        return client.getDatabase(config.database()).getCollection("players", PlayerData.class);
    }

    @AutoService(Codec.class)
    public static final class PlayerDataCodec implements Codec<PlayerData> {
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
    }
}
