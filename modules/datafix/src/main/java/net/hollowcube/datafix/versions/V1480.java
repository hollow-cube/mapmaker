package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V1480 extends DataVersion {
    private static final Map<String, String> CORAL_IDS;

    public V1480() {
        super(1480);

        var blockFix = new BlockRenameFix(CORAL_IDS);
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix(CORAL_IDS));
    }

    static {
        CORAL_IDS = Map.ofEntries(
                Map.entry("minecraft:blue_coral", "minecraft:tube_coral_block"),
                Map.entry("minecraft:pink_coral", "minecraft:brain_coral_block"),
                Map.entry("minecraft:purple_coral", "minecraft:bubble_coral_block"),
                Map.entry("minecraft:red_coral", "minecraft:fire_coral_block"),
                Map.entry("minecraft:yellow_coral", "minecraft:horn_coral_block"),
                Map.entry("minecraft:blue_coral_plant", "minecraft:tube_coral"),
                Map.entry("minecraft:pink_coral_plant", "minecraft:brain_coral"),
                Map.entry("minecraft:purple_coral_plant", "minecraft:bubble_coral"),
                Map.entry("minecraft:red_coral_plant", "minecraft:fire_coral"),
                Map.entry("minecraft:yellow_coral_plant", "minecraft:horn_coral"),
                Map.entry("minecraft:blue_coral_fan", "minecraft:tube_coral_fan"),
                Map.entry("minecraft:pink_coral_fan", "minecraft:brain_coral_fan"),
                Map.entry("minecraft:purple_coral_fan", "minecraft:bubble_coral_fan"),
                Map.entry("minecraft:red_coral_fan", "minecraft:fire_coral_fan"),
                Map.entry("minecraft:yellow_coral_fan", "minecraft:horn_coral_fan"),
                Map.entry("minecraft:blue_dead_coral", "minecraft:dead_tube_coral"),
                Map.entry("minecraft:pink_dead_coral", "minecraft:dead_brain_coral"),
                Map.entry("minecraft:purple_dead_coral", "minecraft:dead_bubble_coral"),
                Map.entry("minecraft:red_dead_coral", "minecraft:dead_fire_coral"),
                Map.entry("minecraft:yellow_dead_coral", "minecraft:dead_horn_coral")
        );
    }
}
