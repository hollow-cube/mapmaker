package net.hollowcube.mapmaker.map.util.datafix;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

public class V4442 extends DataVersion {

    // This seems like it should be a mojang fix, but they don't have an equivalent.
    // This is also the wrong version by far (it should be in 42xx somewhere), but
    // we need to fix it in a more recent map.
    // For those reasons this exists in a mapmaker datafix not inside datafix itself with other mojang fixes.
    public V4442() {
        super(4442);

        addFix(DataTypes.BLOCK_STATE, new BlockStatePropertiesFix(
                "minecraft:creaking_heart", V4442::fixCreakingHeartCreakingToState));
    }

    private static void fixCreakingHeartCreakingToState(Value blockProperties) {
        var creaking = blockProperties.remove("creaking").as(String.class, "disabled");
        blockProperties.put("creaking_heart_state", switch (creaking) {
            case "disabled" -> "uprooted";
            case "dormant" -> "dormant";
            case "active" -> "awake";
            default -> "uprooted"; // fallback
        });
    }
}
