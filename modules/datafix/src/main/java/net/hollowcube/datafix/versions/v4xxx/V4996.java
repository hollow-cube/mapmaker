package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4996 extends DataVersion {
    private static final String[] POT_SIDES = {"back", "left", "right", "front"};

    public V4996() {
        super(4996);

        addReference(DataTypes.DATA_COMPONENTS, field -> field
                .single("minecraft:pot_decorations.back", DataTypes.ITEM_STACK)
                .single("minecraft:pot_decorations.left", DataTypes.ITEM_STACK)
                .single("minecraft:pot_decorations.right", DataTypes.ITEM_STACK)
                .single("minecraft:pot_decorations.front", DataTypes.ITEM_STACK));

        addFix(DataTypes.DATA_COMPONENTS, V4996::fixPotDecorationsComponent);
    }

    private static Value fixPotDecorationsComponent(Value dataComponents) {
        var potDecorations = dataComponents.get("minecraft:pot_decorations");
        if (potDecorations.isNull()) return null;
        dataComponents.put("minecraft:pot_decorations", unpackPotDecorations(potDecorations));
        return null;
    }

    static Value unpackPotDecorations(Value decorations) {
        if (!decorations.isListLike()) return decorations;

        var result = Value.emptyMap();
        for (int i = 0; i < POT_SIDES.length; i++) {
            // Matches vanilla: missing or non-string entries become brick, but an empty id aborts the fix.
            var decorationId = decorations.get(i).as(String.class, "minecraft:brick");
            if (decorationId.isEmpty()) return decorations;

            var stack = Value.emptyMap();
            stack.put("id", decorationId);
            result.put(POT_SIDES[i], stack);
        }
        return result;
    }

}
