package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;

import java.util.Map;

public class V1515 extends DataVersion {
    private static final Map<String, String> RENAMED_CORAL_FANS;

    public V1515() {
        super(1515);

        var blockFix = new BlockRenameFix(RENAMED_CORAL_FANS);
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
    }

    static {
        RENAMED_CORAL_FANS = Map.of(
                "minecraft:tube_coral_fan", "minecraft:tube_coral_wall_fan",
                "minecraft:brain_coral_fan", "minecraft:brain_coral_wall_fan",
                "minecraft:bubble_coral_fan", "minecraft:bubble_coral_wall_fan",
                "minecraft:fire_coral_fan", "minecraft:fire_coral_wall_fan",
                "minecraft:horn_coral_fan", "minecraft:horn_coral_wall_fan"
        );
    }
}
