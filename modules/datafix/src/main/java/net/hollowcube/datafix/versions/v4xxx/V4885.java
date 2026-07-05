package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4885 extends DataVersion {

    public V4885() {
        super(4885);

        // Vanilla RemoveBlockEntityTagFix(minecraft:bed): the bed block entity type was removed.
        // Strip its data component from items carrying it. The chunk-level removal (dropping a bed
        // block entity from the chunk) is not expressible here since PolarDataFixer applies fixes per
        // block entity and cannot remove one from the chunk; the bed block entity is vestigial/empty.
        addFix(DataTypes.DATA_COMPONENTS, V4885::removeBedBlockEntityData);
    }

    private static Value removeBedBlockEntityData(Value dataComponents) {
        var blockEntityData = dataComponents.get("minecraft:block_entity_data");
        if (blockEntityData.getValue("id") instanceof String id && id.equals("minecraft:bed"))
            dataComponents.remove("minecraft:block_entity_data");
        return null;
    }

}
