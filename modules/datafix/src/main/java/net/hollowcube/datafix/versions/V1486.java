package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.fixes.SimpleEntityRenameFix;

import java.util.Map;

public class V1486 extends DataVersion {
    public static final Map<String, String> RENAMED_ENTITY_IDS = Map.of(
            "minecraft:salmon_mob", "minecraft:salmon",
            "minecraft:cod_mob", "minecraft:cod"
    );
    public static final Map<String, String> RENAMED_EGG_IDS = Map.of(
            "minecraft:salmon_mob_spawn_egg", "minecraft:salmon_spawn_egg",
            "minecraft:cod_mob_spawn_egg", "minecraft:cod_spawn_egg"
    );

    public V1486() {
        super(1486);

        renameReference(DataType.ENTITY, "minecraft:cod_mob", "minecraft:cod");
        renameReference(DataType.ENTITY, "minecraft:salmon_mob", "minecraft:salmon");

        addFix(DataType.ENTITY_NAME, new SimpleEntityRenameFix(RENAMED_ENTITY_IDS));
        addFix(DataType.ITEM_NAME, new ItemRenameFix(RENAMED_EGG_IDS));
    }
}
