package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V2679 extends DataVersion {
    public V2679() {
        super(2679);

        // TODO: look for other cases we need to also support flatBlockState where mojang doesnt.
        // should be any usage of BlockStatePropertiesFix
        addFix(DataTypes.BLOCK_STATE, V2679::fixCauldronName);
    }

    private static @Nullable Value fixCauldronName(Value blockState) {
        if (!"minecraft:cauldron".equals(blockState.getValue("Name")))
            return blockState;

        Value properties = blockState.get("Properties");
        if ("0".equals(properties.get("level").as(String.class, "0")))
            blockState.remove("Properties");
        else blockState.put("Name", "minecraft:water_cauldron");

        return null;
    }
}
