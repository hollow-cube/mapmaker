package net.hollowcube.mapmaker.storage;

import com.google.auto.service.AutoService;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.MapQuery;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.MapCodec;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.inc;

public class MapStorageMongo implements MapStorage {

    private final MongoClient client;
    private final MongoConfig config;

    public MapStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull MapData createMap(@NotNull MapData map) {
        try {
            collection().insertOne(map);
        } catch (MongoWriteException err) {
            if (err.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new DuplicateEntryError();
            }
        }
        return map;
    }

    @Override
    public @NotNull MapData getMapById(@NotNull String mapId) {
        var filter = eq("_id", mapId);
        var result = collection().find(filter).limit(1).first();
        if (result == null)
            throw new NotFoundError();
        return result;
    }

    @Override
    public void updateMap(@NotNull MapData map) {
        var filter = eq("_id", map.getId());
        var result = collection().replaceOne(filter, map);
        if (result.getMatchedCount() == 0)
            throw new NotFoundError(map.getId());
    }

    @Override
    public @NotNull MapData deleteMap(@NotNull String mapId) {
        var filter = eq("_id", mapId);
        var result = collection().findOneAndDelete(filter);
        if (result == null)
            throw new NotFoundError();
        return result;
    }

    @Override
    public @NotNull String lookupShortId(@NotNull String shortMapId) {
        var filter = eq("publishedId", shortMapId);
        var projection = include("_id");
        var result = collection().find(filter).projection(projection).limit(1).first();
        if (result == null)
            throw new NotFoundError();
        return result.getId();
    }

    @Override
    public @NotNull List<MapData> getLatestMaps(int offset, int size) {
        var filter = exists("publishedAt");
        var sort = descending("publishedAt");
        var result = collection().find(filter).sort(sort).skip(offset).limit(size).into(new ArrayList<>());
        return result;
    }

    @Override
    public @NotNull List<MapData> queryMaps(@NotNull MapQuery query, int offset, int size) {
        var conditions = new ArrayList<Bson>();
        if (query.isQueryMap() == null || query.query() == null || Boolean.FALSE.equals(query.publishedOnly()))
            return new ArrayList<>();
        if (query.isQueryMap()) {
            if (Boolean.TRUE.equals(query.exactlyMatching())) {
                conditions.add(eq("name", query.query()));
            } else {
                conditions.add(Filters.regex("name", query.query()));
            }
        } else {
            // TODO distinguish exactly matching, we only have UUID to use so more work to do here
            conditions.add(eq("owner", query.query()));
        }
        if (query.publishedOnly() != null)
            conditions.add(query.publishedOnly() ? exists("publishedAt") : not(exists("publishedAt")));
        var filter = and(conditions);
        var sort = query.publishedOnly() != null && query.publishedOnly() ? descending("publishedAt") : descending("createdAt");
        return collection().find(filter)
                .sort(sort)
                .skip(offset)
                .limit(size)
                .into(new ArrayList<>());
    }

    @Override
    public @NotNull String getNextId() {
        var filter = new Document();
        var update = inc("nextId", 1);
        var result = shortIdCollection().findOneAndUpdate(filter, update);
        if (result == null) {
            // Document does not exist
            shortIdCollection().insertOne(new Document("nextId", 1));
            return "00001";
        }
        var n = result.getInteger("nextId");
        var id = "00000" + Integer.toString(n, 36);
        return id.substring(id.length() - 5);
    }

    private @NotNull MongoCollection<MapData> collection() {
        return client.getDatabase(config.database()).getCollection("maps", MapData.class);
    }

    private @NotNull MongoCollection<Document> shortIdCollection() {
        return client.getDatabase(config.database()).getCollection("id_inc");
    }

    @AutoService(Codec.class)
    public static class MapDataCodec implements Codec<MapData> {
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
                    case "spawnPoint" -> {
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
                            var id = reader.readString("id");
                            reader.readName();
                            reader.readStartDocument();
                            var pos = new Vec(
                                    reader.readDouble("x"),
                                    reader.readDouble("y"),
                                    reader.readDouble("z"));
                            reader.readEndDocument();
                            reader.readName(); // data
                            var data = decoderContext.decodeWithChildContext(new MapCodec(), reader);

                            value.addPOI(new MapData.POI(type, id, pos, data));
                            reader.readEndDocument();
                        }
                        reader.readEndArray();
                    }
                    case "completionTimes" -> {
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
                    case "publishedAt" -> value.setPublishedAt(Instant.ofEpochMilli(reader.readDateTime()));
                    case "publishedId" -> value.setPublishedId(reader.readString());
                    case "legacy" -> value.setLegacy(reader.readBoolean());
                    case "icon" -> {
                        Material material = Material.fromNamespaceId(reader.readString());
                        if (material != null) {
                            value.setIcon(ItemStack.of(material));
                        }
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
            writer.writeStartDocument("spawnPoint");
            writer.writeDouble("x", value.getSpawnPoint().x());
            writer.writeDouble("y", value.getSpawnPoint().y());
            writer.writeDouble("z", value.getSpawnPoint().z());
            writer.writeDouble("yaw", value.getSpawnPoint().yaw());
            writer.writeDouble("pitch", value.getSpawnPoint().pitch());
            writer.writeEndDocument();
            writer.writeStartArray("pois");
            for (var poi : value.getPois()) {
                writer.writeStartDocument();
                writer.writeString("type", poi.getType());
                writer.writeString("id", poi.getId());
                writer.writeStartDocument("pos");
                writer.writeDouble("x", poi.getPos().x());
                writer.writeDouble("y", poi.getPos().y());
                writer.writeDouble("z", poi.getPos().z());
                writer.writeEndDocument();
                writer.writeName("data");
                encoderContext.encodeWithChildContext(new MapCodec(), writer, poi.getData());
                writer.writeEndDocument();
            }
            writer.writeEndArray();
            writer.writeStartArray("completionTimes");
            for (var completion : value.getCompletionTimes()) {
                writer.writeStartDocument();
                writer.writeString("uuid", completion.playerUUID().toString());
                writer.writeInt64("timestamp", completion.timeInMills());
                writer.writeEndDocument();
            }
            writer.writeEndArray();
            if (value.isPublished()) {
                writer.writeDateTime("publishedAt", value.getPublishedAt().toEpochMilli());
                writer.writeString("publishedId", value.getPublishedId());
            }
            if (value.isLegacy()) {
                writer.writeBoolean("legacy", true);
            }
            if (value.getIcon() != null) {
                writer.writeString("icon", value.getIcon().material().namespace().toString());
            }
            writer.writeEndDocument();
        }

        @Override
        public Class<MapData> getEncoderClass() {
            return MapData.class;
        }
    }
}
