package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;

public class V3800 extends DataVersion {
    public V3800() {
        super(3800);

        addFix(DataType.ITEM_NAME, new ItemRenameFix("minecraft:scute", "minecraft:turtle_scute"));
    }
}
