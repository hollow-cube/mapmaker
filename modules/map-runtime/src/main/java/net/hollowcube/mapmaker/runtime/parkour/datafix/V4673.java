package net.hollowcube.mapmaker.runtime.parkour.datafix;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.datafix.util.Value;

@AutoService(ExternalDataFix.class)
public class V4673 extends DataVersion implements ExternalDataFix {

    public V4673() {
        super(4673);

        addFix(DataTypes.ENTITY, "minecraft:marker", entity -> {
            var data = entity.get("data");
            var bouncePadData = data.get("bounce_pad");

            if (bouncePadData.isMapLike()) {
                var status = Value.emptyMap();
                var actions = Value.emptyList();
                var action = Value.emptyMap();

                data.put("type", "mapmaker:status");
                data.put("status", status);

                status.put("actions", actions);
                status.put("repeatable", true);
                actions.put(action);
                action.put("type", "mapmaker:velocity");
                action.put("modifier", getModifier(bouncePadData));
            }

            return null;
        });
    }

    private static Value getModifier(Value data) {
        var modifier = Value.emptyMap();
        if (!data.get("power").isNull()) {
            modifier.put("power", data.get("power").as(Double.class, 25.0));
        } else {
            var dx = data.get("dx").as(String.class, null);
            var dy = data.get("dy").as(String.class, null);
            var dz = data.get("dz").as(String.class, null);

            if (dx != null && dy != null && dz != null) {
                modifier.put("dx", dx);
                modifier.put("dy", dy);
                modifier.put("dz", dz);
            }
        }
        return modifier;
    }
}
