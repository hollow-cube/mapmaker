package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4290 extends DataVersion {
    public V4290() {
        super(4290);

        // TODO we need to add the recursive text component schema here.
    }

    private static Value fixParseJsonComponents(Value value) {
        if (value.value() instanceof String raw) {
            // TODO: parse raw from json, should use Codec for this probably.
        }
        return null;
    }

}
