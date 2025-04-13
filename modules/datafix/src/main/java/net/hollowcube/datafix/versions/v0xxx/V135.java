package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V135 extends DataVersion {
    public V135() {
        super(135);

        addReference(DataType.ENTITY_TREE, field -> field
                .list("Passengers", DataType.ENTITY_TREE));

        addFix(DataType.ENTITY, V135::fixEntityRidingToPassengers);
    }

    private static Value fixEntityRidingToPassengers(Value field) {
        while (true) {
            Value riding = field.get("Riding");
            if (!riding.isMapLike()) return field;

            field.put("Riding", null);

            Value passengers = Value.emptyList();
            passengers.add(field);
            riding.put("Passengers", passengers);
            field = riding;
        }
    }
}
