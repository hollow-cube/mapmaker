package net.hollowcube.datafix.versions.v5xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.DataFixUtils;
import net.hollowcube.datafix.util.Value;

public class V5007 extends DataVersion {

    public V5007() {
        super(5007);

        addFix(DataTypes.DATA_COMPONENTS, V5007::fixSwingAnimationSplit);
    }

    private static Value fixSwingAnimationSplit(Value dataComponents) {
        var swingAnimation = dataComponents.remove("minecraft:swing_animation");
        if (swingAnimation.isNull()) return null;
        dataComponents.put("minecraft:attack_animation", swingAnimation);
        dataComponents.put("minecraft:interact_animation", DataFixUtils.deepCopy(swingAnimation));
        return null;
    }

}
