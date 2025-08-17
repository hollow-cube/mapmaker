package net.hollowcube.mapmaker.runtime.parkour.datafix;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.datafix.util.Value;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@AutoService(ExternalDataFix.class)
public class V4437 extends DataVersion implements ExternalDataFix {

    public V4437() {
        super(4437);

        addFix(DataTypes.BLOCK_ENTITY, "mapmaker:checkpoint_plate", V4437::updateItemActionPlaceableOn);
        addFix(DataTypes.BLOCK_ENTITY, "mapmaker:status_plate", V4437::updateItemActionPlaceableOn);
    }

    static Value updateItemActionPlaceableOn(@NotNull Value container) {
        for (var action : container.get("actions")) {
            if (!"mapmaker:give_item".equals(action.getValue("type")))
                continue;

            var block = action.get("block").as(String.class, "minecraft:stone");
            if (Block.fromState(block) == null) continue; // Sanity

            var placeableOn = action.get("placeable_on");
            if (placeableOn.size(0) < 1) {
                // Create the placeable list
                placeableOn = Value.emptyList();
                placeableOn.put(block);
                placeableOn.put("minecraft:target");
                action.put("placeable_on", placeableOn);
            } else {
                // Add itself to placeable on
                placeableOn.put(block);
            }
        }
        return null;
    }

}
