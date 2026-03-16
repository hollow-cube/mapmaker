package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V3097 extends DataVersion {
    public V3097() {
        super(3097);

        addFix(DataTypes.ITEM_STACK, "minecraft:writable_book", V3097::fixRemoveFilteredTextFromBook);
        addFix(DataTypes.ITEM_STACK, "minecraft:written_book", V3097::fixRemoveFilteredTextFromBook);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:sign", V3097::fixRemoveFilteredTextFromSign);

        addFix(DataTypes.ENTITY, "minecraft:cat", V3097::fixCatVariantBritish);
    }

    private static @Nullable Value fixRemoveFilteredTextFromBook(Value itemStack) {
        var tag = itemStack.get("tag");
        tag.remove("filtered_title");
        tag.remove("filtered_pages");
        return null;
    }

    private static @Nullable Value fixRemoveFilteredTextFromSign(Value blockEntity) {
        blockEntity.remove("FilteredText1");
        blockEntity.remove("FilteredText2");
        blockEntity.remove("FilteredText3");
        blockEntity.remove("FilteredText4");
        return null;
    }

    private static @Nullable Value fixCatVariantBritish(Value entity) {
        if ("minecraft:british".equals(entity.getValue("variant")))
            entity.put("variant", "minecraft:british_shorthair");
        return null;
    }
}
