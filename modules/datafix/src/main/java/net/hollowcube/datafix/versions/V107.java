package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V107 extends DataVersion {
    public V107() {
        super(107);

        removeReference(DataType.ENTITY, "Minecart");
    }
}
