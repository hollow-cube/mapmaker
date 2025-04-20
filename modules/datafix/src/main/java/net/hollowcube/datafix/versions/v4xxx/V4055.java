package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.List;

public class V4055 extends DataVersion {
    private static final List<String> PREFIXES = List.of(
            "minecraft:generic.",
            "minecraft:horse.",
            "minecraft:player.",
            "minecraft:zombie."
    );

    public V4055() {
        super(4055);

        addFix(DataTypes.DATA_COMPONENTS, V4055::fixAttributeIdsInDataComponents);
        addFix(DataTypes.ENTITY, V4055::fixAttributeIdsInEntity);
    }

    public static Value fixAttributeIdsInDataComponents(Value dataComponents) {
        for (var modifier : dataComponents.get("minecraft:attribute_modifiers").get("modifiers")) {
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
        for (String prefix : PREFIXES) {
            if (value.startsWith(prefix)) {
                return "minecraft:" + value.substring(prefix.length());
            }
        }
        return value;
    }
}
