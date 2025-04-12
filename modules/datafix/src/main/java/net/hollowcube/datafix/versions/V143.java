package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.entity.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V143 extends DataVersion {
    public V143() {
        super(143);

        removeReference(DataType.ENTITY, "TippedArrow");

        addFix(DataType.ENTITY, "TippedArrow", new EntityRenameFix(V143::fixTippedArrowName));
    }

    private static String fixTippedArrowName(Value ignored, String id) {
        return "TippedArrow".equals(id) ? "Arrow" : id;
    }
}
