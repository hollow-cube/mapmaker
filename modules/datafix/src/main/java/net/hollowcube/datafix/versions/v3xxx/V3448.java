package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3448 extends DataVersion {
    public V3448() {
        super(3448);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:decorated_pot", field -> field
            .list("sherds", DataTypes.ITEM_NAME)
            .single("item", DataTypes.ITEM_STACK));

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:decorated_pot", V3448::fixSherdFieldRename);
    }

    private static Value fixSherdFieldRename(Value blockEntity) {
        blockEntity.put("sherds", blockEntity.remove("shards"));
        return blockEntity;
    }
}
