package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.entity.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V701 extends DataVersion {
    public V701() {
        super(701);

        addReference(DataType.ENTITY, "WitherSkeleton");
        addReference(DataType.ENTITY, "Stray");

        addFix(DataType.ENTITY, "Skeleton", new EntityRenameFix(V701::fixSkeletonSplit));
    }

    private static String fixSkeletonSplit(Value value, String s) {
        int skeletonType = value.get("SkeletonType").as(Number.class, 0).intValue();
        value.put("SkeletonType", null);

        return switch (skeletonType) {
            case 1 -> "WitherSkeleton";
            case 2 -> "Stray";
            default -> s;
        };
    }
}
