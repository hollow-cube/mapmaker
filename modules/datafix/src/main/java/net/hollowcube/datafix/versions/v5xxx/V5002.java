package net.hollowcube.datafix.versions.v5xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V5002 extends DataVersion {

    public V5002() {
        super(5002);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:sign", V5002::fixSignAllowOpFeatures);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:hanging_sign", V5002::fixSignAllowOpFeatures);
    }

    private static Value fixSignAllowOpFeatures(Value sign) {
        sign.put("allow_op_features", true);
        return null;
    }

}
