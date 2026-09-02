package net.hollowcube.anticheat.protocol;

import java.util.ArrayList;
import java.util.List;

/// `play update_attributes`, keyed per entity: each snapshot is the whole instance the client
/// replaces its own with (`ClientPacketListener#handleUpdateAttributes` sets the base, drops every
/// modifier it had and adds these).
public sealed interface S2CUpdateAttributes extends EntityKeyed permits S2CUpdateAttributes.V776 {

    int ADD_VALUE = 0;
    int ADD_MULTIPLIED_BASE = 1;
    int ADD_MULTIPLIED_TOTAL = 2;

    List<Snapshot> attributes();

    /// `attribute` is the attribute registry id.
    record Snapshot(int attribute, double base, List<Modifier> modifiers) {}

    record Modifier(String id, double amount, int operation) {}

    record V776(int entityId, List<Snapshot> attributes) implements S2CUpdateAttributes {

        public static V776 decode(ByteReader reader) {
            int entityId = reader.varInt();
            int count = reader.varInt();
            var attributes = new ArrayList<Snapshot>(count);
            for (int i = 0; i < count; i++) {
                int attribute = reader.varInt();
                double base = reader.f64();
                int modifierCount = reader.varInt();
                var modifiers = new ArrayList<Modifier>(modifierCount);
                for (int j = 0; j < modifierCount; j++) modifiers.add(new Modifier(reader.utf(), reader.f64(), reader.varInt()));
                attributes.add(new Snapshot(attribute, base, List.copyOf(modifiers)));
            }
            return new V776(entityId, List.copyOf(attributes));
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).varInt(attributes.size());
            for (var snapshot : attributes) {
                writer.varInt(snapshot.attribute()).f64(snapshot.base()).varInt(snapshot.modifiers().size());
                for (var modifier : snapshot.modifiers()) writer.utf(modifier.id()).f64(modifier.amount()).varInt(modifier.operation());
            }
        }
    }
}
