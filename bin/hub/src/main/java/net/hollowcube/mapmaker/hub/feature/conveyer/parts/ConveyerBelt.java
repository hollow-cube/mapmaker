package net.hollowcube.mapmaker.hub.feature.conveyer.parts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.hollowcube.mapmaker.hub.feature.conveyer.ConveyerGood;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;

public final class ConveyerBelt implements ConveyerPart {
    private final Point min;
    private final Point max;
    private final Direction direction;
    private final List<ConveyerPart> children;
    private final ConveyerPart parent;
    private final List<WrappedGood> wrappedGoods;
    private final Point movementVector;
    private final Point from;
    private int tick = 0;

    public ConveyerBelt(
            Point min,
            Point max,
            Direction direction,
            Point handOverPoint,
            ConveyerPart parent,
            ConveyerPart... children
    ) {
        parent.children().add(this);
        this.min = direction.negative() ? max : min;
        this.max = direction.negative() ? min : max;
        this.from = handOverPoint;
        this.direction = direction;
        this.children = new ArrayList<>(Arrays.asList(children));
        this.parent = parent;
        this.wrappedGoods = new ArrayList<>();
        this.movementVector = direction.vec().mul(4); // we move goods at 4 blocks/second
    }

    @Override
    public HandOverResult handOver(ConveyerGood good) {
        var wrappedGood = new WrappedGood(good);
        this.wrappedGoods.add(wrappedGood);
        tick(wrappedGood);
        return HandOverResult.ACCEPT;
    }

    @Override
    public void tick(MapInstance instance) {
        if (tick++ % 20 == 0) {
            final Block block;
            if ((tick / 20) % 2 == 0) block = Block.RED_CONCRETE;
            else block = Block.BLUE_CONCRETE;
            //instance.setBlockArea(Area.cuboid(min, max), block);
        }


        var iterator = wrappedGoods.iterator();
        while (iterator.hasNext()) {
            var good = iterator.next();
            if (good.offsetFromStart.lengthSquared() >= good.maxOffset.lengthSquared()) {
                iterator.remove();
                good.nextPart.handOver(good.good);
                continue;
            }
            tick(good);
        }
    }

    private void tick(WrappedGood good) {
        if (good.offsetFromStart.add(movementVector.div(20)).lengthSquared() > good.maxOffset.lengthSquared()) {
            good.updateProgress(good.maxOffset);
        } else {
            good.updateProgress(good.offsetFromStart.add(movementVector.div(20)));
        }
    }


    @Override public List<ConveyerPart> children() {
        return children;
    }

    @Override
    public Point handOverPoint() {
        return this.from;
    }

    @Override
    public String toString() {
        return "ConveyerBelt[" +
                "min=" + min + ", " +
                "max=" + max + ", " +
                "direction=" + direction + ", " +
                "children=" + children + ']';
    }

    final class WrappedGood {
        private final ConveyerGood good;
        private final ConveyerPart nextPart;
        private final Point handOverPoint;
        private final Vec maxOffset;
        private Vec offsetFromStart = Vec.ZERO;

        WrappedGood(ConveyerGood good) {
            this.good = good;
            this.nextPart = children.stream()
                    .filter((child) -> child.getDestinations().contains(this.good.destination()))
                    .findFirst()
                    .orElseThrow();
            this.handOverPoint = nextPart.handOverPoint();
            this.maxOffset = this.handOverPoint.asVec().sub(from);
        }

        public void updateProgress(Vec newOffset) {
            this.offsetFromStart = newOffset;
            this.good.good().teleport(from.add(offsetFromStart).asPos().add(0.5, 0.5, 0.5));
        }

        @Override
        public String toString() {
            return "WrappedGood[" +
                    "good=" + good + ", " +
                    "progress=" + offsetFromStart + ']';
        }
    }

}
