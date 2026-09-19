package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4996_1 extends DataVersion {

    public V4996_1() {
        super(4996, 1);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:decorated_pot", field -> field
                .single("sherds.back", DataTypes.ITEM_STACK)
                .single("sherds.left", DataTypes.ITEM_STACK)
                .single("sherds.right", DataTypes.ITEM_STACK)
                .single("sherds.front", DataTypes.ITEM_STACK)
                .single("item", DataTypes.ITEM_STACK));

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:decorated_pot", V4996_1::fixDecoratedPotSherds);
    }

    private static Value fixDecoratedPotSherds(Value blockEntity) {
        var sherds = blockEntity.get("sherds");
        blockEntity.put("sherds", V4996.unpackPotDecorations(sherds.isNull() ? Value.emptyList() : sherds));
        return null;
    }

}
