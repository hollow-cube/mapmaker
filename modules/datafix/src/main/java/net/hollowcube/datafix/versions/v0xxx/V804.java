package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V804 extends DataVersion {
    public V804() {
        super(804);

        addFix(DataTypes.ITEM_STACK, "minecraft:banner", V804::fixBannerColor);
    }

    private static Value fixBannerColor(Value value) {
        Value tag = value.get("tag");
        if (tag.value() == null) return null;
        Value blockEntityTag = tag.get("BlockEntityTag");
        if (blockEntityTag.value() == null) return null;
        if (!(blockEntityTag.getValue("Base") instanceof Number base))
            return null;

        value.put("Base", null);
        value.put("Damage", (short) (base.intValue() & 15));

        var display = tag.get("display");
        if (display.value() != null) {
            var lore = Value.emptyList();
            lore.put("(+NBT)");
            display.put("Lore", lore);
        }

        return null;
    }
}
