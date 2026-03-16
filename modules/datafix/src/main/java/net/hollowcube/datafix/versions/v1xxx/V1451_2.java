package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V1451_2 extends DataVersion {
    public V1451_2() {
        super(1451, 2);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:piston", field -> field
            .single("blockState", DataTypes.BLOCK_STATE));

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:piston", V1451_2::fixPistonBlockEntityBlockState);
    }

    private static @Nullable Value fixPistonBlockEntityBlockState(Value blockEntity) {
        int legacyBlockId = blockEntity.remove("blockId").as(Number.class, 0).intValue();
        int legacyBlockData = blockEntity.remove("blockData").as(Number.class, 0).intValue();
        var blockState = V1450.getBlockState(legacyBlockId, legacyBlockData);
        if (blockState != null) blockEntity.put("blockState", blockState);
        return null;
    }
}
