package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V143 extends DataVersion {
    public V143() {
        super(143);

        removeReference(DataType.ENTITY, "TippedArrow");
    }
}
