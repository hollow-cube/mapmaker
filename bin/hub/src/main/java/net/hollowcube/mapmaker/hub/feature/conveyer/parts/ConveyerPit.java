package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.ArrayList;
import java.util.List;

import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerItemModel;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

public class ConveyerPit implements ConveyerPart {
    private final Point from;
    private final ConveyerPart parent;
    private final int maxDistance;
    private final ConveyerEnd end;
    private List<WrappedGood> goods = new ArrayList<>();
    private final List<ConveyerPart> child;
    private static final Vec GRAVITY = new Vec(0, -0.08, 0);
    private static final Vec DRAG = new Vec(0, 0.98, 0);
    private static final Vec TERMINAL_VELOCITY = new Vec(0, 7, 0);

    public ConveyerPit(
            Point handOverPoint,
            ConveyerPart parent,
            int maxDistance,
            ConveyerEnd end
    ) {
        parent.children().add(this);
        this.from = handOverPoint;
        this.child = List.of(end);
        this.parent = parent;
        this.maxDistance = maxDistance;
        this.end = end;
    }


    @Override
    public HandOverResult handOver(Point point, ConveyerGood good) {
        this.goods.add(new WrappedGood(good));
        return HandOverResult.ACCEPT;
    }

    @Override
    public void tick(MapInstance instance) {
        var iterator = this.goods.iterator();
        while (iterator.hasNext()) {
            var good = iterator.next();
            good.velocity = good.velocity.add(GRAVITY).mul(DRAG).min(TERMINAL_VELOCITY);
            good.good().teleport(good.good().getPosition().add(good.velocity));
            if (good.good().getPosition().distance(this.from) > this.maxDistance) {
                this.end.handOver(good.good);
                iterator.remove();
            }
        }
    }

    @Override
    public List<ConveyerPart> children() {
        return this.child;
    }

    @Override
    public Point handOverPoint() {
        return this.from;
    }

    class WrappedGood {
        final ConveyerGood good;
        Vec velocity = Vec.ZERO;

        WrappedGood(ConveyerGood good) {
            this.good = good;
        }

        public ConveyerItemModel good() {
            return this.good.good();
        }
    }
}
