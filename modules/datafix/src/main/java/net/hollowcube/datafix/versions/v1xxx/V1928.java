package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.fixes.SimpleEntityRenameFix;

public class V1928 extends DataVersion {
    public V1928() {
        super(1928);

        removeReference(DataType.ENTITY, "minecraft:illager_beast");
        addReference(DataType.ENTITY, "minecraft:ravager");

        addFix(DataType.ITEM_NAME, new ItemRenameFix("minecraft:illager_beast_spawn_egg", "minecraft:ravager_spawn_egg"));
        addFix(DataType.ENTITY_NAME, new SimpleEntityRenameFix("minecraft:illager_beast", "minecraft:ravager"));
    }
}
