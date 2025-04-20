package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.fixes.SimpleEntityRenameFix;

public class V2509 extends DataVersion {
    public V2509() {
        super(2509);

        removeReference(DataTypes.ENTITY, "minecraft:zombie_pigman");
        addReference(DataTypes.ENTITY, "minecraft:zombified_piglin");

        addFix(DataTypes.ENTITY_NAME, new SimpleEntityRenameFix("minecraft:zombie_pigman", "minecraft:zombified_piglin"));
        addFix(DataTypes.ITEM_NAME, new ItemRenameFix("minecraft:zombie_pigman_spawn_egg", "minecraft:zombified_piglin_spawn_egg"));
    }
}
