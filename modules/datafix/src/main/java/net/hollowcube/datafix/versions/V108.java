package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.UUID;

public class V108 extends DataVersion {

    public V108() {
        super(108);

        addFix(DataType.ENTITY, V108::fixEntityStringUuid);
    }

    private static Value fixEntityStringUuid(Value entity) {
        if (!(entity.get("UUID").value() instanceof String uuidString))
            return null;
        entity.put("UUID", null);
        UUID uuid = UUID.fromString(uuidString);
        entity.put("UUIDMost", uuid.getMostSignificantBits());
        entity.put("UUIDLeast", uuid.getLeastSignificantBits());
        return null;
    }

}
