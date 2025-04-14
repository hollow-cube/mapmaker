package net.hollowcube.datafix.versions.v3xxx;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3086 extends DataVersion {
    private static final Int2ObjectMap<String> CAT_VARIANT_IDS = new Int2ObjectArrayMap<>();
    private static final Int2ObjectMap<String> FROG_VARIANT_IDS = new Int2ObjectArrayMap<>();

    public V3086() {
        super(3086);

        addFix(DataTypes.ENTITY, "minecraft:cat", V3086::fixCatVariant);
    }

    private static Value fixCatVariant(Value entity) {
        int catType = entity.remove("CatType").as(Number.class, -1).intValue();
        if (catType == -1) return null;

        entity.put("variant", CAT_VARIANT_IDS.get(catType));
        return null;
    }

    static {
        CAT_VARIANT_IDS.defaultReturnValue("minecraft:tabby");
        CAT_VARIANT_IDS.put(0, "minecraft:tabby");
        CAT_VARIANT_IDS.put(1, "minecraft:black");
        CAT_VARIANT_IDS.put(2, "minecraft:red");
        CAT_VARIANT_IDS.put(3, "minecraft:siamese");
        CAT_VARIANT_IDS.put(4, "minecraft:british");
        CAT_VARIANT_IDS.put(5, "minecraft:calico");
        CAT_VARIANT_IDS.put(6, "minecraft:persian");
        CAT_VARIANT_IDS.put(7, "minecraft:ragdoll");
        CAT_VARIANT_IDS.put(8, "minecraft:white");
        CAT_VARIANT_IDS.put(9, "minecraft:jellie");
        CAT_VARIANT_IDS.put(10, "minecraft:all_black");

        FROG_VARIANT_IDS.defaultReturnValue("minecraft:temperate");
        FROG_VARIANT_IDS.put(0, "minecraft:temperate");
        FROG_VARIANT_IDS.put(1, "minecraft:warm");
        FROG_VARIANT_IDS.put(2, "minecraft:cold");
    }
}
