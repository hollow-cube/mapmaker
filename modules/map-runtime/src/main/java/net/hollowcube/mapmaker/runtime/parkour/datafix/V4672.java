package net.hollowcube.mapmaker.runtime.parkour.datafix;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.datafix.util.Value;

@AutoService(ExternalDataFix.class)
public class V4672 extends DataVersion implements ExternalDataFix {

    public V4672() {
        super(4672);

        addFix(DataTypes.BLOCK_ENTITY, "mapmaker:bounce_pad", blockEntity -> {
            var actions = Value.emptyList();
            var action = Value.emptyMap();

            blockEntity.put("id", "mapmaker:status_plate");
            blockEntity.put("actions", actions);

            actions.put(action);
            action.put("type", "mapmaker:velocity");
            action.put("modifier", getModifier(blockEntity));

            return null;
        });

        addFix(DataTypes.ENTITY, "minecraft:marker", entity -> {
            var data = entity.get("data");
            var type = data.get("type").as(String.class, "");

            if (type.equals("mapmaker:bounce_pad")) {
                var status = Value.emptyMap();
                var actions = Value.emptyList();
                var action = Value.emptyMap();

                data.put("type", "mapmaker:status");
                data.put("status", status);

                status.put("actions", actions);
                actions.put(action);
                action.put("type", "mapmaker:velocity");
                action.put("modifier", getModifier(data));
            }

            return null;
        });

        addFix(DataTypes.ITEM_STACK, stack -> {
            var components = stack.get("components");
            var data = components.get("minecraft:custom_data");

            if (data.get("mapmaker:handler").as(String.class, "").equals("mapmaker:bounce_pad")) {
                data.remove("mapmaker:handler");

                var lore = Value.emptyList();
                var line = Value.emptyMap();
                line.put("color", "red");
                line.put("text", "Moved into an action in status plates.");
                lore.put(line);
                components.put("minecraft:lore", lore);

                var title = Value.emptyMap();
                title.put("color", "red");
                title.put("text", "Removed Item (mapmaker:bounce_pad)");
                components.put("minecraft:custom_name", title);
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
