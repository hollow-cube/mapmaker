package net.hollowcube.anticheat.protocol;

import java.util.ArrayList;
import java.util.List;

/// `common update_tags`. Applied immediately in the play phase and buffered until
/// `finish_configuration` in the configuration phase; either way the whole set is state the reader
/// has to replay, so the entries are decoded in wire order rather than into a map.
public sealed interface S2CUpdateTags extends Packet permits S2CUpdateTags.V776 {

    List<RegistryTags> registries();

    record RegistryTags(String registry, List<Tag> tags) {
    }

    record Tag(String name, int[] entries) {
    }

    record V776(List<RegistryTags> registries) implements S2CUpdateTags {

        public static V776 decode(ByteReader reader) {
            int count = count(reader);
            var registries = new ArrayList<RegistryTags>(count);
            for (int i = 0; i < count; i++) registries.add(decodeRegistry(reader));
            return new V776(List.copyOf(registries));
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(registries.size());
            for (var registry : registries) {
                writer.utf(registry.registry()).varInt(registry.tags().size());
                for (var tag : registry.tags()) writer.utf(tag.name()).varIntArray(tag.entries());
            }
        }

        private static RegistryTags decodeRegistry(ByteReader reader) {
            var registry = reader.utf();
            int count = count(reader);
            var tags = new ArrayList<Tag>(count);
            for (int i = 0; i < count; i++) tags.add(new Tag(reader.utf(), reader.varIntArray()));
            return new RegistryTags(registry, List.copyOf(tags));
        }

        /// Bounded by what is left in the frame, so a corrupt count cannot make the decoder allocate.
        private static int count(ByteReader reader) {
            int count = reader.varInt();
            if (count < 0 || count > reader.remaining()) throw new ProtocolException("bad tag count: " + count);
            return count;
        }
    }
}
