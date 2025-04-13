package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.LegacyAttributeRenameFix;

import java.util.Map;

public class V3814 extends DataVersion {
    public V3814() {
        super(3814);

        var fix = new LegacyAttributeRenameFix(Map.of("minecraft:horse.jump_strength", "minecraft:generic.jump_strength"));
        addFix(DataType.ITEM_STACK, fix::fixInItemStack);
        addFix(DataType.ENTITY, fix::fixInEntity);
    }
}
