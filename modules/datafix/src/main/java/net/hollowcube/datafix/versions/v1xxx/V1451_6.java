package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import net.hollowcube.datafix.versions.v0xxx.V102;

public class V1451_6 extends DataVersion {
    public V1451_6() {
        super(1451, 6);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:jukebox", V1451_6::fixJukeboxBlockEntity);
    }

    private static Value fixJukeboxBlockEntity(Value blockEntity) {
        var record = blockEntity.remove("Record").as(Number.class, 0).intValue();
        if (record <= 0) return null;

        var recordItemId = V1451_4.updateItemId(V102.ITEM_NAMES.get(record), 0);
        if (recordItemId == null) return null;

        var recordItem = Value.emptyMap();
        recordItem.put("id", recordItemId);
        recordItem.put("Count", (byte) 1);
        blockEntity.put("RecordItem", recordItem);

        return null;
    }
}
