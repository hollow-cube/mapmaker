package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V3093 extends DataVersion {
    public V3093() {
        super(3093);

        addFix(DataTypes.ENTITY, "minecraft:goat", V3093::fixGoatLeftRightHorn);
    }

    private static @Nullable Value fixGoatLeftRightHorn(Value entity) {
        entity.put("HasLeftHorn", true);
        entity.put("HasRightHorn", true);
        return null;
    }
}
