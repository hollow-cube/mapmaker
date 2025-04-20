package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V700 extends DataVersion {
    public V700() {
        super(700);

        addReference(DataTypes.ENTITY, "ElderGuardian");

        addFix(DataTypes.ENTITY, "Guardian", new EntityRenameFix(V700::fixElderGuardianSplit));
    }

    private static String fixElderGuardianSplit(Value value, String s) {
        return "Guardian".equals(s) && value.remove("Elder").as(Boolean.class, false)
                ? "ElderGuardian" : s;
    }
}
