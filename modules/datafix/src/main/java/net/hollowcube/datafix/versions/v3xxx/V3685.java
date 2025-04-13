package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V3685 extends DataVersion {
    public V3685() {
        super(3685);

        addReference(DataType.ENTITY, "minecraft:trident", V3685::abstractArrow);
        addReference(DataType.ENTITY, "minecraft:spectral_arrow", V3685::abstractArrow);
        addReference(DataType.ENTITY, "minecraft:arrow", V3685::abstractArrow);
    }

    static @NotNull Field abstractArrow(@NotNull Field field) {
        return field
                .single("inBlockState", DataType.BLOCK_STATE)
                .single("item", DataType.ITEM_STACK);
    }
}
