package net.hollowcube.mapmaker.map.util.datafix;

import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class V4439 extends DataVersion {

    public V4439() {
        super(4439);

        addFix(HCDataTypes.PLAY_STATE, V4439::updateHotbarPlaceableOn);
    }

    private static Value updateHotbarPlaceableOn(@NotNull Value data) {
        var hotbar = data.get("mapmaker:hotbar_items");
        updateHotbarPlaceableOnForItem(hotbar.get("item0"));
        updateHotbarPlaceableOnForItem(hotbar.get("item1"));
        updateHotbarPlaceableOnForItem(hotbar.get("item2"));
        return null;
    }

    private static void updateHotbarPlaceableOnForItem(@NotNull Value data) {
        if (data.get("item").as(String.class, "").equals("mapmaker:block")) {
            var block = data.get("block").as(String.class, "minecraft:stone");
            if (Block.fromState(block) == null) return; // Sanity check

            var placeableOn = data.get("placeable_on");
            if (placeableOn.size(0) < 1) {
                // Create the placeable list
                placeableOn = Value.emptyList();
                placeableOn.put(block);
                placeableOn.put("minecraft:target");
                data.put("placeable_on", placeableOn);
            } else {
                // Add itself to placeable on
                placeableOn.put(block);
            }
        }
    }
}
