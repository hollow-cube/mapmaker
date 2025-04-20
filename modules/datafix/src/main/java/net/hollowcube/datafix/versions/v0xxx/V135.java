package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V135 extends DataVersion {
    public V135() {
        super(135);

        addReference(DataTypes.ENTITY_TREE, field -> field
                .list("Passengers", DataTypes.ENTITY_TREE));

        addFix(DataTypes.ENTITY, V135::fixEntityRidingToPassengers);
    }

    private static Value fixEntityRidingToPassengers(Value field) {
        while (true) {
            Value riding = field.get("Riding");
            if (!riding.isMapLike()) return field;

            field.put("Riding", null);

            Value passengers = Value.emptyList();
            passengers.put(field);
            riding.put("Passengers", passengers);
            field = riding;
        }
    }
}
