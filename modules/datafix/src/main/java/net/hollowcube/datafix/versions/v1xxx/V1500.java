package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataVersion;

public class V1500 extends DataVersion {
    public V1500() {
        super(1500);

        // TODO: This has BlockEntityKeepPacked which adds keepPacked to
        //  a block handler with the id of "DUMMY", not sure what this is
        //  used for. need to investigate more.
    }
}
