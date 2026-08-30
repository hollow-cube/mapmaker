package net.hollowcube.anticheat.world;

import net.hollowcube.anticheat.protocol.ByteReader;
import net.hollowcube.anticheat.protocol.Nbt;
import net.hollowcube.anticheat.protocol.ProtocolException;
import net.hollowcube.anticheat.protocol.S2CRegistryData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The `minecraft:dimension_type` entries of the current configuration phase, in wire order, since
/// `CommonPlayerSpawnInfo#dimensionTypeId` is an index into exactly that order.
///
/// Only `min_y` and `height` are read. The heights cannot come from the chunk packet instead: a
/// chunk gives the section count but never the bottom section, and `block_update` carries an
/// absolute y that has to be turned into a section index. So the entry payload is walked with the
/// minimal NBT reader below, which collects the root compound's int fields and skips everything
/// else without interpreting it.
///
/// Entries with no payload are the ones the client already has from a known pack; those fall back
/// to the vanilla values in [DimensionInfo].
public final class DimensionRegistry {

    private static final Map<String, DimensionInfo> BUILT_IN = Map.of(
        DimensionInfo.OVERWORLD.id(), DimensionInfo.OVERWORLD,
        DimensionInfo.OVERWORLD_CAVES.id(), DimensionInfo.OVERWORLD_CAVES,
        DimensionInfo.THE_NETHER.id(), DimensionInfo.THE_NETHER,
        DimensionInfo.THE_END.id(), DimensionInfo.THE_END);

    private List<DimensionInfo> entries = List.of();

    public void apply(S2CRegistryData packet) {
        if (!S2CRegistryData.DIMENSION_TYPE_REGISTRY.equals(packet.registry())) return;
        var result = new ArrayList<DimensionInfo>(packet.entries().size());
        for (var entry : packet.entries()) result.add(read(entry));
        entries = List.copyOf(result);
    }

    /// Cleared at every `start_configuration`, because the client builds a fresh `RegistryAccess`
    /// from each configuration phase and a server that did not resend the registries would break it
    /// too.
    public void clear() {
        entries = List.of();
    }

    public int size() {
        return entries.size();
    }

    /// The dimension the given `dimensionTypeId` selects, falling back to the overworld shape when
    /// the registry was never seen (a tap installed mid-session) or the index is out of range.
    public DimensionInfo get(int index) {
        return index >= 0 && index < entries.size() ? entries.get(index) : DimensionInfo.OVERWORLD;
    }

    private static DimensionInfo read(S2CRegistryData.Entry entry) {
        var fallback = BUILT_IN.getOrDefault(entry.id(), DimensionInfo.OVERWORLD);
        byte[] data = entry.data();
        if (data == null) return new DimensionInfo(entry.id(), fallback.minY(), fallback.height());

        Map<String, Integer> ints;
        try {
            ints = rootInts(new ByteReader(data));
        } catch (ProtocolException _) {
            return new DimensionInfo(entry.id(), fallback.minY(), fallback.height());
        }
        return new DimensionInfo(entry.id(),
            ints.getOrDefault("min_y", fallback.minY()),
            ints.getOrDefault("height", fallback.height()));
    }

    /// The int fields of a network-form root compound. Anything that is not a root-level int is
    /// skipped by length.
    private static Map<String, Integer> rootInts(ByteReader reader) {
        var result = new HashMap<String, Integer>();
        if (reader.u8() != Nbt.TAG_COMPOUND) return result;
        int type;
        while ((type = reader.u8()) != Nbt.TAG_END) {
            var name = Nbt.name(reader);
            if (type == Nbt.TAG_INT) result.put(name, reader.i32());
            else Nbt.skipPayload(reader, type);
        }
        return result;
    }
}
