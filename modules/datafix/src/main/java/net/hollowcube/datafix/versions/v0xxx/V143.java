package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V143 extends DataVersion {
    public V143() {
        super(143);

        removeReference(DataTypes.ENTITY, "TippedArrow");

        addFix(DataTypes.ENTITY, "TippedArrow", new EntityRenameFix(V143::fixTippedArrowName));
    }

    private static String fixTippedArrowName(Value ignored, String id) {
        return "TippedArrow".equals(id) ? "Arrow" : id;
    }
}
