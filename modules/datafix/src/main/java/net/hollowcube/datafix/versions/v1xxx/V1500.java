package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V1500 extends DataVersion {

    public V1500() {
        super(1500);

        // Not sure what the dummy block entity is used for, but we add this anyway :)
        addFix(DataTypes.BLOCK_ENTITY, "DUMMY", V1500::fixAddKeepPackedToDummyBlockEntity);
    }

    private static @Nullable Value fixAddKeepPackedToDummyBlockEntity(Value blockEntity) {
        blockEntity.put("keepPacked", true);
        return null;
    }
}
