package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V1904 extends DataVersion {
    public V1904() {
        super(1904);

        addReference(DataType.ENTITY, "minecraft:cat");

        addFix(DataType.ENTITY, "minecraft:ocelot", new EntityRenameFix(V1904::fixEntityCatName));
    }

    private static String fixEntityCatName(Value entity, String id) {
        int catType = entity.get("CatType").as(Number.class, 0).intValue();
        if (catType == 0) {
            var owner = entity.get("Owner").as(String.class, "");
            var ownerUuid = entity.get("OwnerUUID").as(String.class, "");
            if (!owner.isEmpty() || !ownerUuid.isEmpty()) {
                entity.put("Trusting", true);
            }
        } else if (catType > 0 && catType < 4) {
            entity.put("CatType", catType);
            entity.put("OwnerUUID", entity.get("OwnerUUID").as(String.class, ""));
            return "minecraft:cat";
        }

        return id;
    }
}
