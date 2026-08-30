package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// `configuration registry_data`. Only the entry names are read; each entry's contents stay as the
/// raw network NBT bytes, which is enough for the world model to find its dimension type by name
/// and for a reader to parse the rest later.
public sealed interface S2CRegistryData extends Packet permits S2CRegistryData.V776 {

    String DIMENSION_TYPE_REGISTRY = "minecraft:dimension_type";

    String registry();

    List<Entry> entries();

    /// One registry element: its id, and the network NBT that describes it, absent when the client
    /// already has it from a known pack.
    record Entry(String id, byte @Nullable [] data) {
    }

    record V776(String registry, List<Entry> entries) implements S2CRegistryData {

        public static V776 decode(ByteReader reader) {
            var registry = reader.utf();
            int count = reader.varInt();
            if (count < 0 || count > reader.remaining())
                throw new ProtocolException("bad registry entry count: " + count);
            var entries = new ArrayList<Entry>(count);
            for (int i = 0; i < count; i++) entries.add(decodeEntry(reader));
            return new V776(registry, List.copyOf(entries));
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.utf(registry).varInt(entries.size());
            for (var entry : entries) {
                writer.utf(entry.id()).bool(entry.data() != null);
                if (entry.data() != null) writer.bytes(entry.data());
            }
        }

        /// The payload is kept verbatim rather than parsed, so it is only walked far enough to find
        /// where it ends.
        private static Entry decodeEntry(ByteReader reader) {
            var id = reader.utf();
            if (!reader.bool()) return new Entry(id, null);
            int start = reader.index();
            reader.skipNbt();
            return new Entry(id, reader.since(start));
        }
    }
}
