package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V109 extends DataVersion {

    public V109() {
        super(109);

        addFix(DataTypes.ENTITY, V109::fixEntityHealth);
    }

    private static Value fixEntityHealth(Value entity) {
        if (entity.getValue("HealF") instanceof Number n) {
            entity.put("HealF", null);
            entity.put("Health", n.floatValue());
        } else if (entity.getValue("Health") instanceof Number n) {
            entity.put("Health", n.floatValue());
        }
        return null;
    }

}
