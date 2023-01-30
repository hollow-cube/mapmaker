package net.hollowcube.mapmaker.storage;

import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.model.SaveState;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.jglrxavpok.hephaistos.nbt.NBTReader;
import org.jglrxavpok.hephaistos.nbt.NBTWriter;
import org.jglrxavpok.hephaistos.parser.SNBTParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.descending;

public class SaveStateStorageMongo implements SaveStateStorage {
    private final MongoClient client;
    private final MongoConfig config;

    public SaveStateStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull ListenableFuture<@NotNull SaveState> createSaveState(@NotNull SaveState saveState) {
        return Futures.submit(() -> {
            try {
                collection().insertOne(saveState);
                return saveState;
            } catch (DuplicateKeyException ignored) {
                throw new SaveStateStorage.NotFoundError();
            }
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull ListenableFuture<Void> updateSaveState(@NotNull SaveState saveState) {
        return Futures.submit(() -> {
            var filter = eq("_id", saveState.getId());
            var result = collection().replaceOne(filter, saveState);
            if (result.getModifiedCount() == 0)
                throw new NotFoundError();
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull ListenableFuture<@NotNull SaveState> getLatestSaveState(@NotNull String playerId, @NotNull String mapId) {
        return Futures.submit(() -> {
            var filter = and(
                    eq("playerId", playerId),
                    eq("mapId", mapId),
                    or(eq("complete", false), not(exists("complete"))));
            var sort = descending("startTime");
            var result = collection().find(filter, SaveState.class).sort(sort).limit(1).first();
            if (result == null)
                throw new NotFoundError();
            return result;
        }, ForkJoinPool.commonPool());
    }

    private @NotNull MongoCollection<SaveState> collection() {
        return client.getDatabase(config.database()).getCollection("savestates", SaveState.class);
    }

    @AutoService(Codec.class)
    public static final class SaveStateCodec implements Codec<SaveState> {
        @Override
        public SaveState decode(BsonReader reader, DecoderContext decoderContext) {
            var value = new SaveState();
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                switch (reader.readName()) {
                    case "_id" -> value.setId(reader.readString());
                    case "playerId" -> value.setPlayerId(reader.readString());
                    case "mapId" -> value.setMapId(reader.readString());
                    case "editing" -> value.setEditing(true);
                    case "completed" -> value.setCompleted(reader.readBoolean());
                    case "startTime" -> value.setStartTime(Instant.ofEpochMilli(reader.readDateTime()));
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
                    case "checkpoint" -> value.setCheckpoint(reader.readString());
                    case "inventory" -> {
                        reader.readStartArray();
                        List<ItemStack> inv = new ArrayList<>();
                        while (reader.readBsonType() != BsonType.NULL && reader.readBsonType() != BsonType.STRING) {
                            if (reader.readBsonType() == null) {
                                inv.add(ItemStack.AIR);
                            } else {
                                try {
                                    var itemNbt = (NBTCompound) new SNBTParser(new StringReader(reader.readString())).parse();
                                    inv.add(ItemStack.fromItemNBT(itemNbt));
                                } catch (NBTException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        value.setInventory(inv);
                        reader.readEndArray();
                    }
                    case "nbt" -> {
                        var data = reader.readBinaryData().getData();
                        try (var nbtReader = new NBTReader(new ByteArrayInputStream(data))) {
                            value.setNbt((NBTCompound) nbtReader.read());
                        } catch (IOException | NBTException e) {
                            // Will not throw reading from a byte array
                            throw new RuntimeException(e);
                        }
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
            writer.writeString("playerId", value.getPlayerId());
            writer.writeString("mapId", value.getMapId());
            if (value.isEditing())
                writer.writeBoolean("editing", true);
            if (value.isCompleted())
                writer.writeBoolean("completed", true);
            writer.writeDateTime("startTime", value.getStartTime().toEpochMilli());
            writer.writeInt64("playtime", value.getPlaytime());
            if (value.getPos() != null) {
                writer.writeStartDocument("pos");
                writer.writeDouble("x", value.getPos().x());
                writer.writeDouble("y", value.getPos().y());
                writer.writeDouble("z", value.getPos().z());
                writer.writeDouble("yaw", value.getPos().yaw());
                writer.writeDouble("pitch", value.getPos().pitch());
                writer.writeEndDocument();
            }
            if (value.getCheckpoint() != null) {
                writer.writeString("checkpoint", value.getCheckpoint());
            }
            if (value.getInventory() != null && !value.getInventory().isEmpty()) {
                writer.writeStartArray("inventory");
                for (var item : value.getInventory()) {
                    if (item == ItemStack.AIR) {
                        writer.writeNull();
                    } else {
                        writer.writeString(item.toItemNBT().toSNBT());
                    }
                }
                writer.writeEndArray();
            }
            if (value.getNbt() != null) {
                var bos = new ByteArrayOutputStream();
                try (var nbtWriter = new NBTWriter(bos)){
                    nbtWriter.writeRaw(value.getNbt());
                } catch (IOException e) {
                    // Will not throw for writing to a byte array
                    throw new RuntimeException(e);
                }
                writer.writeBinaryData("nbt", new BsonBinary(bos.toByteArray()));
            }
            writer.writeEndDocument();
        }

        @Override
        public Class<SaveState> getEncoderClass() {
            return SaveState.class;
        }
    }
}
