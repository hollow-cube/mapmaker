package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V107 extends DataVersion {
    public V107() {
        super(107);

        removeReference(DataType.ENTITY, "Minecart");

        addFix(DataType.ENTITY, "Minecart", new EntityRenameFix(V107::fixMinecartId));
    }

    private static String fixMinecartId(Value value, String id) {
        if (!id.equals("Minecart"))
            return id;

        int type = value.get("Type").as(Number.class, 0).intValue();
        value.put("Type", null);
        return switch (type) {
            case 1 -> "MinecartChest";
            case 2 -> "MinecartFurnace";
            default -> "MinecartRideable";
        };
    }
}
