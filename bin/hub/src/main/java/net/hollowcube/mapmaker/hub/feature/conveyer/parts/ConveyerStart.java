package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class ConveyerStart implements ConveyerPart {
    private final List<ConveyerPart> children = new ArrayList<>();
    int tick = 0;
    int goods = 0;

    @Override
    public HandOverResult handOver(ConveyerGood good) {
        return HandOverResult.REJECT;
    }

    @Override
    public void tick(MapInstance instance) {
        if (tick++ % 40 != 0) {
            return;
        }

        var targets = getDestinations();
        for (var child : children) {
            var goodDisplay = new NpcItemModel(UUID.randomUUID());
            var meta = goodDisplay.getEntityMeta();
            goodDisplay.setInstance(instance);
            meta.setItemStack(ItemStack.of(Material.DIRT));
            meta.setPosRotInterpolationDuration(1);
            meta.setTransformationInterpolationDuration(1);
            child.handOver(new ConveyerGood(goodDisplay, this, targets.get(Math.floorMod(goods++, targets.size()))));
        }
    }

    @Override
    public List<ConveyerPart> children() {
        return children;
    }

    @Override
    public Point handOverPoint() {
        return Vec.ZERO;
    }
}
