package net.hollowcube.apiworker.index;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.datafix.DataType;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.block.CheckpointPlateBlock;
import net.hollowcube.mapmaker.runtime.parkour.block.StatusPlateBlock;
import net.hollowcube.mapmaker.runtime.parkour.marker.CheckpointMarkerHandler;
import net.hollowcube.mapmaker.runtime.parkour.marker.FinishMarkerHandler;
import net.hollowcube.mapmaker.runtime.parkour.marker.StatusMarkerHandler;
import net.hollowcube.polar.PolarChunk;
import net.hollowcube.polar.PolarWorld;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/// Finds the checkpoint, finish and status triggers in a world, and counts what else lives in it.
///
/// Triggers live in three places. The spawn checkpoint is on the world user data and holds
/// whatever the player starts with; markers are entities carrying a region, in the chunk user
/// data; plates are pressure plate blocks, stored as block entities. All three are decoded with
/// the same [ActionTriggerData] codec the runtime uses, so an action means here exactly what it
/// means in game.
///
/// Old worlds are upgraded on the way through. Block entities are already fixed by the time polar
/// hands them over, but the world and chunk user data are mapmaker's own formats and have to be
/// run through [HCDataTypes] here, against the data version stored on the world.
///
/// Anything that fails to decode is dropped and counted rather than thrown, since one unreadable
/// trigger should cost that trigger and not the whole map. A chunk whose user data does not read
/// at all costs everything in it — its markers and its decoration alike — and counts as one.
final class TriggerScan {
    private static final Logger logger = LoggerFactory.getLogger(TriggerScan.class);

    /// Chunk user data at or below this version stored entities in a bespoke binary format rather
    /// than NBT. Maps that old are skipped rather than given a second parser.
    private static final int VERSION_PRE_CHUNK_NBT = ReadWorldAccess.VERSION_PRE_CHUNK_NBT;
    private static final int VERSION_PRE_WORLD_NBT = ReadWorldAccess.VERSION_PRE_WORLD_NBT;

    // Plate block entity ids. The plate blocks keep theirs private, so these are spelled here.
    private static final String PLATE_CHECKPOINT = "mapmaker:checkpoint_plate";
    private static final String PLATE_FINISH = "mapmaker:finish_plate";
    private static final String PLATE_STATUS = "mapmaker:status_plate";

    /// Where a marker keeps its trigger data, under the marker's own `data` compound; the same key
    /// the plate of that kind uses for its block entity.
    private static final String DATA_CHECKPOINT = CheckpointPlateBlock.ENTITY_DATA_TAG.key();
    private static final String DATA_STATUS = StatusPlateBlock.ENTITY_DATA_TAG.key();
    /// The spawn checkpoint, on the world tag rather than anywhere in the world.
    private static final String SPAWN_EFFECTS = ParkourMapWorld.SPAWN_CHECKPOINT_EFFECTS.key();

    /// Markers are the one entity type that is structure rather than decoration.
    private static final String ENTITY_MARKER = "minecraft:marker";
    private static final String ENTITY_TEXT_DISPLAY = "minecraft:text_display";

    enum Kind {
        CHECKPOINT, FINISH, STATUS,
        /// The map's starting state, which is not a place in the world.
        SPAWN;

        /// Whether two overlapping regions of this kind are one trigger. Overlapping finish
        /// regions are how you draw a shape out of boxes, but two overlapping checkpoints are
        /// deliberately two places the run can be saved.
        boolean mergesRegions() {
            return this == FINISH;
        }
    }

    /// A trigger after merging. Position is the center of everything that merged into it.
    record Trigger(Kind kind, double x, double y, double z, ActionTriggerData data) {
    }

    /// @param dataVersion    the world's mapmaker data version, or -1 when it predates one
    /// @param entities       entities that are not markers, passengers included
    /// @param textDisplays   how many of those are text displays
    /// @param decodeFailures triggers dropped for not decoding, and so missing from `triggers`
    record Scan(int dataVersion, List<Trigger> triggers, int entities, int textDisplays, int decodeFailures) {
    }

    /// Running totals over a scan, passed down through the chunk readers.
    private static final class Counts {
        int failures;
        int entities;
        int textDisplays;
    }

    static Scan scan(PolarWorld world) {
        var counts = new Counts();

        // The data version lives on the world user data and is what every later fix is measured
        // against, so the world has to be read before any chunk.
        int dataVersion = dataVersion(world);

        var raw = new ArrayList<Raw>();
        for (var chunk : world.chunks()) {
            readEntities(chunk, dataVersion, raw, counts);
            readPlates(chunk, raw, counts);
        }

        var triggers = merge(raw);
        var spawn = readSpawn(world, dataVersion, counts);
        if (spawn != null) triggers.add(spawn);

        if (counts.failures != 0)
            logger.warn("dropped {} trigger(s) that failed to decode", counts.failures);
        return new Scan(dataVersion, triggers, counts.entities, counts.textDisplays, counts.failures);
    }

    // Reading

    /// The world user data version, or -1 when the world predates it being written, in which case
    /// nothing can be upgraded and the data is taken as it stands.
    private static int dataVersion(PolarWorld world) {
        try {
            var buffer = wrap(world.userData());
            if (buffer == null) return -1;

            int version = buffer.read(NetworkBuffer.BYTE);
            if (version <= VERSION_PRE_CHUNK_NBT) return -1;
            return buffer.read(NetworkBuffer.VAR_INT);
        } catch (Exception e) {
            return -1;
        }
    }

    /// Reads the spawn checkpoint, which holds everything the player starts the map with.
    ///
    /// Without it a map that hands out blocks or turns on a rule once at spawn looks like a map
    /// that does neither, which is most of them: per-checkpoint actions are the exception.
    private static @Nullable Trigger readSpawn(PolarWorld world, int dataVersion, Counts counts) {
        try {
            var buffer = wrap(world.userData());
            if (buffer == null) return null;

            int version = buffer.read(NetworkBuffer.BYTE);
            if (version <= VERSION_PRE_WORLD_NBT) return null;
            if (version > VERSION_PRE_CHUNK_NBT) buffer.read(NetworkBuffer.VAR_INT);
            if (!(buffer.read(NetworkBuffer.NBT) instanceof CompoundBinaryTag worldData)) return null;

            var upgraded = upgrade(HCDataTypes.WORLD, worldData, dataVersion);
            if (!upgraded.keySet().contains(SPAWN_EFFECTS)) return null;

            var data = decode(upgraded.getCompound(SPAWN_EFFECTS), counts);
            if (data == null || data.actions().size() == 0) return null;

            // The spawn has no position worth reporting, and counting it as a checkpoint would
            // double count the start of every map.
            return new Trigger(Kind.SPAWN, 0, 0, 0, data);
        } catch (Exception e) {
            counts.failures++;
            return null;
        }
    }

    /// Reads a chunk's entities: markers become triggers, everything else is counted.
    private static void readEntities(PolarChunk chunk, int dataVersion, List<Raw> out, Counts counts) {
        CompoundBinaryTag chunkData;
        try {
            var buffer = wrap(chunk.userData());
            if (buffer == null) return;
            if (buffer.read(NetworkBuffer.VAR_INT) <= VERSION_PRE_CHUNK_NBT) return;
            if (!(buffer.read(NetworkBuffer.NBT) instanceof CompoundBinaryTag tag)) return;
            chunkData = upgrade(HCDataTypes.CHUNK, tag, dataVersion);
        } catch (Exception e) {
            counts.failures++;
            return;
        }

        for (var entry : chunkData.getList("entities", BinaryTagTypes.COMPOUND)) {
            var entity = (CompoundBinaryTag) entry;
            if (!ENTITY_MARKER.equals(entity.getString("id"))) {
                countEntity(entity, counts);
                continue;
            }

            var data = entity.getCompound("data");
            Kind kind = markerKind(data.getString("type"));
            if (kind == null) continue;

            var position = entity.getList("Pos", BinaryTagTypes.DOUBLE);
            if (position.size() < 3) continue;
            double x = position.getDouble(0);
            double y = position.getDouble(1);
            double z = position.getDouble(2);

            // A region is saved as offsets from the marker's position, which is what
            // MarkerEntity#boundingBox adds back; a marker with no region is a point.
            double[] min = corner(data, "min", x, y, z);
            double[] max = corner(data, "max", x, y, z);
            out.add(new Raw(kind, false,
                Math.min(min[0], max[0]), Math.min(min[1], max[1]), Math.min(min[2], max[2]),
                Math.max(min[0], max[0]), Math.max(min[1], max[1]), Math.max(min[2], max[2]),
                markerData(data, kind, counts)));
        }
    }

    /// An entity and whatever rides it: a text display is as often a passenger as a root.
    private static void countEntity(CompoundBinaryTag entity, Counts counts) {
        counts.entities++;
        if (ENTITY_TEXT_DISPLAY.equals(entity.getString("id"))) counts.textDisplays++;
        for (var passenger : entity.getList("Passengers", BinaryTagTypes.COMPOUND))
            countEntity((CompoundBinaryTag) passenger, counts);
    }

    private static void readPlates(PolarChunk chunk, List<Raw> out, Counts counts) {
        for (var blockEntity : chunk.blockEntities()) {
            Kind kind = plateKind(blockEntity.id());
            if (kind == null) continue;

            // A plate's block entity compound is the trigger data itself, with no wrapper, and
            // polar has already run it through the block entity fixes.
            var data = blockEntity.data() == null
                ? ActionTriggerData.EMPTY : decodeOrEmpty(blockEntity.data(), counts);
            double x = chunk.x() * 16 + blockEntity.x();
            double z = chunk.z() * 16 + blockEntity.z();
            out.add(new Raw(kind, true, x, blockEntity.y(), z, x, blockEntity.y(), z, data));
        }
    }

    /// Only checkpoints and status markers carry actions; reaching a finish or a reset volume is
    /// the whole effect, so those handlers have no trigger data to read.
    private static ActionTriggerData markerData(CompoundBinaryTag data, Kind kind, Counts counts) {
        String key = switch (kind) {
            case CHECKPOINT -> DATA_CHECKPOINT;
            case STATUS -> DATA_STATUS;
            case FINISH, SPAWN -> null;
        };
        if (key == null || !data.keySet().contains(key)) return ActionTriggerData.EMPTY;
        return decodeOrEmpty(data.getCompound(key), counts);
    }

    // Decoding

    private static ActionTriggerData decodeOrEmpty(CompoundBinaryTag tag, Counts counts) {
        var data = decode(tag, counts);
        return data == null ? ActionTriggerData.EMPTY : data;
    }

    private static @Nullable ActionTriggerData decode(CompoundBinaryTag tag, Counts counts) {
        try {
            if (ActionTriggerData.CODEC.decode(PolarHelper.CODER, tag) instanceof Result.Ok<ActionTriggerData> ok)
                return ok.value();
            counts.failures++;
            return null;
        } catch (Exception e) {
            counts.failures++;
            return null;
        }
    }

    /// Runs mapmaker's own user data formats forward to the current version. Worlds written before
    /// the data version existed are left alone, which is what the runtime does too.
    private static CompoundBinaryTag upgrade(DataType type, CompoundBinaryTag tag, int dataVersion) {
        if (dataVersion == -1 || dataVersion >= DataFixer.maxVersion()) return tag;
        return (CompoundBinaryTag) DataFixer.upgrade(type, Transcoder.NBT, tag, dataVersion, DataFixer.maxVersion());
    }

    private static @Nullable NetworkBuffer wrap(byte @Nullable [] data) {
        if (data == null || data.length == 0) return null;
        var buffer = NetworkBuffer.wrap(data, 0, data.length);
        buffer.writeIndex(data.length);
        return buffer;
    }

    private static @Nullable Kind markerKind(String type) {
        return switch (type) {
            case CheckpointMarkerHandler.ID -> Kind.CHECKPOINT;
            case FinishMarkerHandler.ID -> Kind.FINISH;
            case StatusMarkerHandler.ID -> Kind.STATUS;
            default -> null;
        };
    }

    private static @Nullable Kind plateKind(@Nullable String id) {
        if (id == null) return null;
        return switch (id) {
            case PLATE_CHECKPOINT -> Kind.CHECKPOINT;
            case PLATE_FINISH -> Kind.FINISH;
            case PLATE_STATUS -> Kind.STATUS;
            default -> null;
        };
    }

    private static double[] corner(CompoundBinaryTag data, String key, double x, double y, double z) {
        var list = data.getList(key, BinaryTagTypes.DOUBLE);
        if (list.size() < 3) return new double[]{x, y, z};
        return new double[]{x + list.getDouble(0), y + list.getDouble(1), z + list.getDouble(2)};
    }

    // Merging

    private record Raw(
        Kind kind, boolean plate,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        ActionTriggerData data
    ) {
    }

    /// Groups triggers that are really one thing and returns one [Trigger] per group.
    ///
    /// Trigger counts run to hundreds at most, so this is a quadratic sweep rather than anything
    /// cleverer. A group takes the data of whichever member has the most actions: a pad of plates
    /// is the same trigger duplicated, so counting each one would inflate every mechanic signal by
    /// the size of the pad.
    private static List<Trigger> merge(List<Raw> raw) {
        int[] group = new int[raw.size()];
        for (int i = 0; i < group.length; i++) group[i] = i;

        for (int i = 0; i < raw.size(); i++) {
            for (int j = i + 1; j < raw.size(); j++) {
                if (find(group, i) == find(group, j)) continue;
                if (mergeable(raw.get(i), raw.get(j))) group[find(group, i)] = find(group, j);
            }
        }

        Int2ObjectMap<List<Raw>> byGroup = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < raw.size(); i++)
            byGroup.computeIfAbsent(find(group, i), _ -> new ArrayList<>()).add(raw.get(i));

        var out = new ArrayList<Trigger>(byGroup.size());
        for (var members : byGroup.values()) {
            double x = 0;
            double y = 0;
            double z = 0;
            var data = ActionTriggerData.EMPTY;
            for (var member : members) {
                x += (member.minX + member.maxX) / 2;
                y += (member.minY + member.maxY) / 2;
                z += (member.minZ + member.maxZ) / 2;
                if (member.data.actions().size() > data.actions().size()) data = member.data;
            }
            out.add(new Trigger(members.getFirst().kind,
                x / members.size(), y / members.size(), z / members.size(), data));
        }
        return out;
    }

    private static boolean mergeable(Raw a, Raw b) {
        if (a.kind != b.kind || a.plate != b.plate) return false;

        // Plates are single blocks, so touching means orthogonally adjacent. Diagonal neighbours
        // are left alone: a pad is face connected, and two checkpoints a step apart are not one.
        if (a.plate) {
            double dx = Math.abs(a.minX - b.minX);
            double dy = Math.abs(a.minY - b.minY);
            double dz = Math.abs(a.minZ - b.minZ);
            return dx + dy + dz == 1;
        }

        if (!a.kind.mergesRegions()) return false;
        return a.minX <= b.maxX && a.maxX >= b.minX
            && a.minY <= b.maxY && a.maxY >= b.minY
            && a.minZ <= b.maxZ && a.maxZ >= b.minZ;
    }

    private static int find(int[] group, int i) {
        while (group[i] != i) i = group[i] = group[group[i]];
        return i;
    }

    private TriggerScan() {
    }
}
