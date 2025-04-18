package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1803 extends DataVersion {
    public V1803() {
        super(1803);

        addFix(DataTypes.ITEM_STACK, V1803::fixItemLore);
    }

    private static Value fixItemLore(Value value) {
        var display = value.get("tag").get("display");
        var lore = display.get("Lore");
        if (lore.isNull()) return null;

        var newLore = Value.emptyList();
        for (Value loreEntry : lore) {
            newLore.put(loreEntry.value() instanceof String s
                    ? Value.wrap("{\"text\":\"" + s + "\"}")
                    : loreEntry);
        }
        display.put("Lore", newLore);

        return null;
    }
}
