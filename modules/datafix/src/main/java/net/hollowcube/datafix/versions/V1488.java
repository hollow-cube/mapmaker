package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.util.Value;

import java.util.Map;

public class V1488 extends DataVersion {
    private static final Map<String, String> RENAMED_BLOCKS_IDS = Map.of(
            "minecraft:kelp_top", "minecraft:kelp",
            "minecraft:kelp", "minecraft:kelp_plant"
    );

    public V1488() {
        super(1488);

        addReference(DataType.BLOCK_ENTITY, "minecraft:command_block", field -> field
                .single("CustomName", DataType.TEXT_COMPONENT)
                .single("LastOutput", DataType.TEXT_COMPONENT));

        var blockFix = new BlockRenameFix(RENAMED_BLOCKS_IDS);
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix("minecraft:kelp_top", "minecraft:kelp"));

        addFix(DataType.BLOCK_ENTITY, "minecraft:command_block", V1488::fixCommandBlockEntityCustomName);
        addFix(DataType.ENTITY, "minecraft:commandblock_minecart", V1488::fixCommandBlockEntityCustomName);
    }

    private static Value fixCommandBlockEntityCustomName(Value value) {
        final String customName = value.get("CustomName").as(String.class, "");
        value.put("CustomName", customName == null ? null : "{\"text\":\"" + customName + "\"}");
        return null;
    }
}
