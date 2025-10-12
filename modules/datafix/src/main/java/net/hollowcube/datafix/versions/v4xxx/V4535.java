package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4535 extends DataVersion {

    public V4535() {
        super(4535);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:copper_golem", V4535::fix);
    }

    private static Value fix(Value block) {
        switch (block.remove("weather_state").as(Integer.class, 0)) {
            case 1 -> block.put("weather_state", "exposed");
            case 2 -> block.put("weather_state", "weathered");
            case 3 -> block.put("weather_state", "oxidized");
            default -> block.put("weather_state", "unaffected");
        }
        return null;
    }

}
