package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4173 extends DataVersion {
    public V4173() {
        super(4173);

        addFix(DataType.ENTITY, "minecraft:tnt_minecart", V4173::fixTntMinecartFuse);
    }

    private static Value fixTntMinecartFuse(Value entity) {
        entity.put("fuse", entity.remove("TNTFuse"));
        return null;
    }
}
