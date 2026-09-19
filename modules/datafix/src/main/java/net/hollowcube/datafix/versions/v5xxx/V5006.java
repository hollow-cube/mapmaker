package net.hollowcube.datafix.versions.v5xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V5006 extends DataVersion {

    public V5006() {
        super(5006);

        addFix(DataTypes.BLOCK_STATE, V5006::fixBlockStateFieldNames);
    }

    private static Value fixBlockStateFieldNames(Value blockState) {
        if (!blockState.isMapLike()) return null;
        renameField(blockState, "Name", "id");
        renameField(blockState, "Properties", "properties");
        return null;
    }

    private static void renameField(Value value, String oldName, String newName) {
        var field = value.remove(oldName);
        if (!field.isNull()) value.put(newName, field);
    }

}
