package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;

public class V820 extends DataVersion {
    public V820() {
        super(820);

        addFix(DataTypes.ITEM_NAME, new ItemRenameFix("minecraft:totem", "minecraft:totem_of_undying"));
    }
}
