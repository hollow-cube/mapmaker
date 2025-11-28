package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.List;

import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.minestom.server.coordinate.Point;

public final class ConveyerEnd implements ConveyerPart {
    private final Point pickupPoint;

    public ConveyerEnd(Point pickupPoint) {
        this.pickupPoint = pickupPoint;
    }

    public List<ConveyerEnd> end = List.of(this);

    @Override
    public HandOverResult handOver(ConveyerGood good) {
        good.good().remove();
        return HandOverResult.ACCEPT;
    }

    @Override
    public void tick(MapInstance instance) {
    }

    @Override
    public List<ConveyerEnd> getDestinations() {
        return end;
    }

    @Override
    public List<ConveyerPart> children() {
        return List.of();
    }

    @Override
    public Point handOverPoint() {
        return this.pickupPoint;
    }

    @Override
    public String toString() {
        return "ConveyerEnd[" +
                "pickupPoint=" + pickupPoint + ']';
    }

}
