package net.hollowcube.datafix.versions.v0xxx;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V107 extends DataVersion {
    // Paper: handle additional minecart cases untouched by vanilla
    private static final Int2ObjectMap<String> MINECART_IDS;

    public V107() {
        super(107);

        removeReference(DataTypes.ENTITY, "Minecart");

        addFix(DataTypes.ENTITY, "Minecart", new EntityRenameFix(V107::fixMinecartId));
    }

    private static String fixMinecartId(Value value, String id) {
        if (!id.equals("Minecart"))
            return id;

        var type = value.remove("Type").as(Number.class, 0);
        return MINECART_IDS.get(type.intValue());
    }

    static {
        MINECART_IDS = new Int2ObjectArrayMap<>();
        MINECART_IDS.defaultReturnValue("MinecartRideable");
        MINECART_IDS.put(1, "MinecartChest");
        MINECART_IDS.put(2, "MinecartFurnace");
        MINECART_IDS.put(3, "MinecartTNT");
        MINECART_IDS.put(4, "MinecartSpawner");
        MINECART_IDS.put(5, "MinecartHopper");
        MINECART_IDS.put(6, "MinecartCommandBlock");
    }
}
