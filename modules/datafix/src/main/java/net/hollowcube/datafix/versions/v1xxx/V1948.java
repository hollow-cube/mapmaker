package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1948 extends DataVersion {
    public V1948() {
        super(1948);

        addFix(DataType.ITEM_STACK, "minecraft:white_banner", V1948::fixOminousBannerName);
    }

    private static Value fixOminousBannerName(Value value) {
        var itemName = value.get("display").get("Name").as(String.class, null);
        if (itemName == null || itemName.isEmpty()) return null;

        itemName = itemName.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"");
        value.get("display").put("Name", itemName);

        return null;
    }
}
