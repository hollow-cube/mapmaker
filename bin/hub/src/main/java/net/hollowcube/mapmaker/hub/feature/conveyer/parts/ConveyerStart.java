package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Stream;

import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerItemModel;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.Material;

public class ConveyerStart implements ConveyerPart {
    private final List<ConveyerPart> children = new ArrayList<>();
    List<BadSprite> types = Stream.of(
            "1ee377ab-661a-4deb-8f9d-d47945f67553",
            "7c1d979c-3660-40fa-a7e3-acf9417411ac",
            "9d258538-0387-4cad-8744-57a777f75c41",
            "0056b7ba-0c79-46f8-ac7b-f516d5c9d1de",
            "437acb57-652a-43f3-97fa-fa9c78422209",
            "485ac31d-db31-4353-b5cf-40eb248f777d",
            "950b5d2c-8740-4e27-a1b9-c97c13067576",
            "9343ea1d-96f8-41e3-8c62-fa7696606700",
            "27100fe5-9aba-419a-ae56-6291fecaa1ed",
            "b792dac2-63b1-4be5-89ba-1ab7524b1095",
            "c09f8d1f-ef34-4ed2-ac4e-b22ea51edae7",
            "edff16b2-d677-4afd-a4cb-18915247a504",
            "ea9367e7-c359-45da-bdb6-1261ef8ae25a",
            "e326db93-5e1d-414d-8b7b-b1e2accc12c5",
            "cc98ee61-15d9-448d-876f-f3205a219a4f"
    ).map((id) -> BadSprite.require("hub/5x5/" + id)).toList();
    private static final AtomicInteger elementCounter = new AtomicInteger();
    int tick = 0;
    int goods = 0;

    @Override
    public HandOverResult handOver(ConveyerGood good) {
        return HandOverResult.REJECT;
    }

    @Override
    public void tick(MapInstance instance) {
        if (tick++ % 80 != 0) {
            return;
        }

        var targets = getDestinations();
        for (var child : children) {
            var goodDisplay = new ConveyerItemModel();
            var meta = goodDisplay.getEntityMeta();
            goodDisplay.setInstance(instance);
            goodDisplay.setModel(Material.LIGHT,
                                 types.get(Math.floorMod(elementCounter.getAndIncrement(), types.size())));
            meta.setPosRotInterpolationDuration(1);
            meta.setTransformationInterpolationDuration(1);
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRDPERSON_RIGHT_HAND);
            meta.setScale(new Vec(4.5, 4.5, 4.5));
            goodDisplay.setSynchronizationTicks(Long.MAX_VALUE);
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
