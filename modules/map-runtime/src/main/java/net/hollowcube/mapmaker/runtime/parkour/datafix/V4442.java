package net.hollowcube.mapmaker.runtime.parkour.datafix;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

@AutoService(ExternalDataFix.class)
public class V4442 extends DataVersion implements ExternalDataFix {

    // This seems like it should be a mojang fix, but they don't have an equivalent.
    // This is also the wrong version by far (it should be in 42xx somewhere), but
    // we need to fix it in a more recent map.
    // For those reasons this exists in a mapmaker datafix not inside datafix itself with other mojang fixes.
    public V4442() {
        super(4442);

        var fixer = new BlockStatePropertiesFix("minecraft:creaking_heart", V4442::fixCreakingHeartCreakingToState);
        addFix(DataTypes.BLOCK_STATE, fixer);
        addFix(DataTypes.FLAT_BLOCK_STATE, fixer);
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
