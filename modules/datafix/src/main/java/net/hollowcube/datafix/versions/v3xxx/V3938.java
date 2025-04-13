package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V3938 extends DataVersion {
    public V3938() {
        super(3938);

        addReference(DataType.ENTITY, "minecraft:spectral_arrow", V3938::abstractArrow);
        addReference(DataType.ENTITY, "minecraft:arrow", V3938::abstractArrow);
    }

    static @NotNull Field abstractArrow(@NotNull Field field) {
        return field
                .single("inBlockState", DataType.BLOCK_STATE)
                .single("item", DataType.ITEM_STACK)
                .single("weapon", DataType.ITEM_STACK);
    }
}
