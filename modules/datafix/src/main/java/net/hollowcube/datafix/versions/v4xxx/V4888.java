package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4888 extends DataVersion {

    public V4888() {
        super(4888);

        addFix(DataTypes.DATA_COMPONENTS, V4888::fixAttributeIdsInDataComponents);
        addFix(DataTypes.ENTITY, V4888::fixAttributeIdsInEntity);
    }

    public static Value fixAttributeIdsInDataComponents(Value dataComponents) {
        // attribute_modifiers is a flat list of modifiers since V4307.
        for (var modifier : dataComponents.get("minecraft:attribute_modifiers")) {
            if (modifier.getValue("type") instanceof String s)
                modifier.put("type", fix(s));
        }
        return null;
    }

    public static Value fixAttributeIdsInEntity(Value entity) {
        for (var attribute : entity.get("attributes")) {
            if (attribute.getValue("id") instanceof String s)
                attribute.put("id", fix(s));
        }
        return null;
    }

    private static String fix(String value) {
        return value.equals("minecraft:nameplate_distance") ? "minecraft:name_tag_distance" : value;
    }

}
