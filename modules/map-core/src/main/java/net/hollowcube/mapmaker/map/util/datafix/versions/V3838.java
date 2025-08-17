package net.hollowcube.mapmaker.map.util.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;
import org.jetbrains.annotations.NotNull;

public class V3838 extends DataVersion {

    public V3838() {
        super(3838);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:checkpoint_plate", V3838::itemHolder);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:checkpoint_plate", V3838::itemHolder);

        // Both states 'added' in this version
        addReference(HCDataTypes.EDIT_STATE, field -> field.single("inventory.*", DataTypes.ITEM_STACK));
        addReference(HCDataTypes.PLAY_STATE, field -> field.list("ghostBlocks", DataTypes.FLAT_BLOCK_STATE));
    }

    static @NotNull DataType.Builder itemHolder(@NotNull DataType.Builder field) {
        return field
                .single("items.item1", DataTypes.ITEM_STACK)
                .single("items.item2", DataTypes.ITEM_STACK)
                .single("items.item3", DataTypes.ITEM_STACK);
    }
}
