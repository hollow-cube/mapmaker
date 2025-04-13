package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1953 extends DataVersion {
    public V1953() {
        super(1953);

        addFix(DataType.BLOCK_ENTITY, "minecraft:banner", V1953::fixOminousBannerBlockEntityName);
    }

    private static Value fixOminousBannerBlockEntityName(Value value) {
        var customName = value.get("CustomName").as(String.class, null);
        if (customName == null || customName.isEmpty()) return null;

        customName = customName.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"");
        value.put("CustomName", customName);

        return null;
    }
}
