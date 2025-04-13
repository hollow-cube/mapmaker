package net.hollowcube.datafix.versions.v3xxx;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3087 extends DataVersion {
    private static final Int2ObjectMap<String> FROG_VARIANT_IDS = new Int2ObjectArrayMap<>();

    public V3087() {
        super(3087);

        addFix(DataType.ENTITY, "minecraft:frog", V3087::fixFrogVariant);
    }

    private static Value fixFrogVariant(Value entity) {
        int frogVariant = entity.remove("Variant").as(Number.class, -1).intValue();
        if (frogVariant == -1) return null;

        entity.put("variant", FROG_VARIANT_IDS.get(frogVariant));
        return null;
    }

    static {
        FROG_VARIANT_IDS.defaultReturnValue("minecraft:temperate");
        FROG_VARIANT_IDS.put(0, "minecraft:temperate");
        FROG_VARIANT_IDS.put(1, "minecraft:warm");
        FROG_VARIANT_IDS.put(2, "minecraft:cold");
    }
}
