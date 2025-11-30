package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.util.Set;

import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.minestom.server.coordinate.Point;

public interface ConveyerPart {

    default HandOverResult handOver(ConveyerGood good) {
        return handOver(handOverPoint(), good);
    }
    HandOverResult handOver(Point handOverPoint, ConveyerGood good);
    void tick(MapInstance instance);
    List<ConveyerPart> children();
    Point handOverPoint();

    default List<ConveyerEnd> getDestinations() {
        var list = new ArrayList<ConveyerEnd>();
        this.children().stream().map(ConveyerPart::getDestinations).forEach(list::addAll);
        return list;
    }

    default Set<ConveyerPart> collectChildren() {
        var parts = new HashSet<ConveyerPart>();
        parts.add(this);
        children().stream().map(ConveyerPart::collectChildren).forEach(parts::addAll);
        return parts;
    }

    default boolean shouldBePaused() {
        return children().stream().anyMatch(ConveyerPart::shouldBePaused);
    }

    enum HandOverResult {
        ACCEPT,
        REJECT,
    }
}
