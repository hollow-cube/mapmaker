package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

public class V3685 extends DataVersion {
    public V3685() {
        super(3685);

        addReference(DataTypes.ENTITY, "minecraft:trident", V3685::abstractArrow);
        addReference(DataTypes.ENTITY, "minecraft:spectral_arrow", V3685::abstractArrow);
        addReference(DataTypes.ENTITY, "minecraft:arrow", V3685::abstractArrow);

        addFix(DataTypes.ENTITY, "minecraft:arrow", V3685::fixArrow);
        addFix(DataTypes.ENTITY, "minecraft:spectral_arrow", V3685::fixSpectralArrow);
    }

    static @NotNull DataType.Builder abstractArrow(@NotNull DataType.Builder field) {
        return field
                .single("inBlockState", DataTypes.BLOCK_STATE)
                .single("item", DataTypes.ITEM_STACK);
    }

    private static Value fixArrow(Value value) {
        var potionId = value.get("Potion").as(String.class, "minecraft:empty");
        value.put("item", createItemStack("minecraft:empty".equals(potionId) ? "minecraft:arrow" : "minecraft:tipped_arrow"));
        return null;
    }

    private static Value fixSpectralArrow(Value value) {
        value.put("item", createItemStack("minecraft:spectral_arrow"));
        return null;
    }

    private static Value createItemStack(String type) {
        var item = Value.emptyMap();
        item.put("id", type);
        item.put("Count", 1);
        return item;
    }
}
