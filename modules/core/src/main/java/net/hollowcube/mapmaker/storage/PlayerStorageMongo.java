package net.hollowcube.mapmaker.storage;

import com.google.auto.service.AutoService;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.model.PlayerData;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;

import static com.mongodb.client.model.Filters.eq;

class PlayerStorageMongo implements PlayerStorage {
    private final MongoClient client;
    private final MongoConfig config;

    public PlayerStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull PlayerData createPlayer(@NotNull PlayerData player) {
        try {
            collection().insertOne(player);
        } catch (DuplicateKeyException ignored) {
            throw new DuplicateEntryError();
        }
        return player;
    }

    @Override
    public @NotNull PlayerData getPlayerByUuid(@NotNull String uuid) {
        var filter = eq("uuid", uuid);
        var result = collection().find(filter).limit(1).first();
        if (result == null)
            throw new NotFoundError(uuid);
        return result;
    }

    @Override
    public void updatePlayer(@NotNull PlayerData player) {
        var filter = eq("_id", player.getId());
        var result = collection().replaceOne(filter, player);
        if (result.getModifiedCount() == 0)
            throw new NotFoundError(player.getId());
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
                    case "username" -> player.setUsername(reader.readString());
                    case "displayName" -> reader.readString(); // Do no save display name, it is just a cache
                    case "unlockedMapSlots" -> player.setUnlockedMapSlots(reader.readInt32());
                    case "mapSlots" -> {
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
            writer.writeString("username", value.getUsername());
            // Do no save display name, it is just a cache
            writer.writeInt32("unlockedMapSlots", value.getUnlockedMapSlots());
            writer.writeStartArray("mapSlots");
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
