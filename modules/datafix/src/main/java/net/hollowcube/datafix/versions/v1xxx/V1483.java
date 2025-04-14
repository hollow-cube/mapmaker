package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.fixes.SimpleEntityRenameFix;

public class V1483 extends DataVersion {
    public V1483() {
        super(1483);

        renameReference(DataTypes.ENTITY, "minecraft:puffer_fish", "minecraft:pufferfish");

        addFix(DataTypes.ITEM_NAME, new ItemRenameFix("minecraft:puffer_fish_spawn_egg", "minecraft:pufferfish_spawn_egg"));
        addFix(DataTypes.ENTITY_NAME, new SimpleEntityRenameFix("minecraft:puffer_fish", "minecraft:pufferfish"));
    }
}
