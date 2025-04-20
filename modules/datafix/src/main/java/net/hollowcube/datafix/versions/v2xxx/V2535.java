package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V2535 extends DataVersion {
    public V2535() {
        super(2535);

        addFix(DataTypes.ENTITY, "minecraft:shulker", V2535::fixShulkerEntityRotation);
    }

    private static Value fixShulkerEntityRotation(Value entity) {
        var rotation = entity.get("Rotation");
        if (rotation.isNull() || rotation.size(0) == 0)
            return null;

        var newRotation = Value.emptyList();
        for (var rotationEntry : rotation)
            newRotation.put(rotationEntry.as(Number.class, 180.0).doubleValue() - 180.0);
        entity.put("Rotation", newRotation);

        return null;
    }
}
