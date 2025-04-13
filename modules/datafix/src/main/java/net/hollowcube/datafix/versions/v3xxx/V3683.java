package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3683 extends DataVersion {
    public V3683() {
        super(3683);

        addReference(DataType.ENTITY, "minecraft:tnt", field -> field
                .single("block_state", DataType.BLOCK_STATE));

        addFix(DataType.ENTITY, "minecraft:tnt", V3683::fixTntFuse);
        addFix(DataType.ENTITY, "minecraft:tnt", V3683::fixAddTntBlockState);
    }

    private static Value fixTntFuse(Value entity) {
        entity.put("fuse", entity.remove("Fuse"));
        return null;
    }

    private static Value fixAddTntBlockState(Value entity) {
        var blockState = Value.emptyMap();
        blockState.put("Name", "minecraft:tnt");
        entity.put("block_state", blockState);
        return null;
    }
}
