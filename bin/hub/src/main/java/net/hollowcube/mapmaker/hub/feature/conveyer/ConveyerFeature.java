package net.hollowcube.mapmaker.hub.feature.conveyer;

import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.HashSet;

import java.util.Set;

import java.util.concurrent.ThreadLocalRandom;

import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.feature.conveyer.parts.*;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.Direction;

@AutoService(HubFeature.class)
public class ConveyerFeature implements HubFeature {

    public static final int RATE = 4; // blocks per second

    private final Set<ConveyerPart> conveyerParts = new HashSet<>();

    public ConveyerFeature() {
    }

    @Override
    public void load(MapServer server, HubMapWorld world) {
        this.conveyerParts.addAll(this.createFirstBelt().collectChildren());
        this.conveyerParts.addAll(this.createSecondBelt(world).collectChildren());
        this.conveyerParts.addAll(this.createThirdBelt(world).collectChildren());
        this.conveyerParts.addAll(this.createRawBelt(world).collectChildren());
        server.scheduler().buildTask(() -> this.tick(world.instance())).repeat(TaskSchedule.tick(1)).schedule();
    }

    private void tick(MapInstance instance) {
        conveyerParts.forEach((part) -> part.tick(instance));
    }

    private ConveyerStart createThirdBelt(HubMapWorld server) {
        var start = new ConveyerStart();

        var firstEnd = new ConveyerEnd(new Pos(-82, 37, -3));
        var secondEnd = new ConveyerEnd(new Pos(-92, 37, -5));

        var firstBelt = new ConveyerBelt(
                new Pos(-71, 36, -63),
                new Pos(-67, 36, -33),
                Direction.SOUTH,
                new Pos(-69, 37, -65),
                start
        );
        var secondBelt = new ConveyerBelt(
                new Pos(-71, 36, -29),
                new Pos(-67, 36, -25),
                Direction.SOUTH,
                new Pos(-69, 37, -30),
                firstBelt
        );

        var firstCrane = new Crane(
                new ArrayList<>(),
                secondBelt,
                Direction.EAST,
                Direction.NORTH,
                Direction.EAST,
                new Pos(-68, 37, -27),
                new Pos(-76, 37, -27),
                server
        );

        var rightSideFirstBelt = new ConveyerBelt(
                new Pos(-99, 36, -36),
                new Pos(-75, 36, -32),
                Direction.WEST,
                new Pos(-76, 37, -34),
                firstCrane
        );
        var secondCrane = new Crane(
                new ArrayList<>(),
                rightSideFirstBelt,
                Direction.NORTH,
                Direction.EAST,
                Direction.EAST,
                new Pos(-98, 37, -34),
                new Pos(-99, 37, -27),
                server
        );
        new ConveyerBelt(
                new Pos(-94, 36, -27),
                new Pos(-90, 36, -8),
                Direction.SOUTH,
                new Pos(-92, 37, -28),
                secondCrane,
                secondEnd
        );

        var leftSideFirstBelt = new ConveyerBelt(
                new Pos(-71, 36, -21),
                new Pos(-67, 36, -8),
                Direction.SOUTH,
                new Pos(-69, 37, -20),
                secondBelt
        );
        new ConveyerBelt(
                new Pos(-79, 36, -5),
                new Pos(-69, 36, -1),
                Direction.WEST,
                new Pos(-69, 37, -3),
                leftSideFirstBelt,
                firstEnd
        );

        return start;
    }

    private ConveyerStart createSecondBelt(HubMapWorld server) {
        var start = new ConveyerStart();

        var firstEnd = new ConveyerEnd(new Pos(-91, 37, 5));
        var secondEnd = new ConveyerEnd(new Pos(-82, 37, 5));

        var firstBelt = new ConveyerBelt(
                new Pos(-74, 36, 45),
                new Pos(-70, 36, 73),
                Direction.NORTH,
                new Pos(-72, 37, 77),
                start
        );
        var secondBelt = new ConveyerBelt(
                new Pos(-74, 36, 37),
                new Pos(-70, 36, 40),
                Direction.NORTH,
                new Pos(-72, 37, 42),
                firstBelt
        );


        var crane = new Crane(
                new ArrayList<>(),
                secondBelt,
                Direction.WEST,
                Direction.NORTH,
                Direction.EAST,
                new Pos(-72, 37, 38),
                new Pos(-65, 37, 38),
                server
        );
        var rightSideFirstBelt = new ConveyerBelt(
                new Pos(-67, 36, 27),
                new Pos(-63, 36, 33),
                Direction.NORTH,
                new Pos(-65, 37, 31),
                crane
        );
        var rightSideSecondBelt = new ConveyerBelt(
                new Pos(-77, 36, 20),
                new Pos(-65, 36, 24),
                Direction.WEST,
                new Pos(-65, 37, 22),
                rightSideFirstBelt
        );
        new ConveyerBelt(
                new Pos(-84, 36, 7),
                new Pos(-80, 36, 22),
                Direction.NORTH,
                new Pos(-82, 37, 22),
                rightSideSecondBelt,
                secondEnd
        );

        var leftSideFirstBelt = new ConveyerBelt(
                new Pos(-87, 36, 30),
                new Pos(-72, 36, 34),
                Direction.WEST,
                new Pos(-72, 37, 32),
                secondBelt
        );
        new ConveyerBelt(
                new Pos(-94, 36, 9),
                new Pos(-92, 36, 32),
                Direction.NORTH,
                new Pos(-92, 37, 32),
                leftSideFirstBelt,
                firstEnd
        );

        return start;
    }

    private ConveyerStart createFirstBelt() {
        var start = new ConveyerStart();
        var end = new ConveyerEnd(new Pos(-58, 34, 57));

        new ConveyerBelt(
                new Pos(-60, 33, -65),
                new Pos(-56, 33, 53),
                Direction.SOUTH,
                new Pos(-58, 34, -68),
                start,
                end
        );

        return start;
    }

    private ConveyerStart createRawBelt(HubMapWorld server) {
        var blobEnd = new ConveyerEnd(Vec.ZERO);
        var normalEnd = new ConveyerEnd(new Pos(-68, 40, -66));
        var start = new ConveyerStart((instance, part) -> {
            var number = ThreadLocalRandom.current().nextInt(1, 100);
            if (number == 1) {
                var cargo = ConveyerStart.CargoSupplier.createCargo(instance);
                cargo.setModel(
                        Material.LIGHT,
                        BadSprite.require("hub/raw/the_creature")
                );
                cargo.setDefaultMeta();
                return new ConveyerGood(cargo, part, blobEnd);
            }

            return ConveyerStart.CargoType.RAW.get(instance, part);
        }, 50);
        start.markNonRandom(blobEnd);

        var firstBelt = new ConveyerBelt(
                new Pos(-28, 39, -38),
                new Pos(9, 39, -34),
                Direction.WEST,
                new Pos(15, 40, -36),
                start
        );

        var secondBelt = new ConveyerBelt(
                new Pos(-35, 39, -67),
                new Pos(-30, 39, -36),
                Direction.NORTH,
                new Pos(-33, 40, -36),
                firstBelt
        );
        var thirdBelt = new ConveyerBelt(
                new Pos(-35, 39, -83),
                new Pos(-31, 39, -72),
                Direction.NORTH,
                new Pos(-33, 40, -72),
                secondBelt
        );
        var piston = new Piston(
                Direction.EAST,
                new Pos(-33, 40, -78),
                new Pos(-33, 40, -78),
                thirdBelt,
                server
        );
        new ConveyerPit(
                new Pos(-27, 40, -78),
                piston,
                100,
                blobEnd
        );

        var fourthBelt = new ConveyerBelt(
                new Pos(-63, 39, -90),
                new Pos(-33, 39, -86),
                Direction.WEST,
                new Pos(-33, 40, -88),
                thirdBelt
        );
        for (int i = 0; i < 5; i++) {
            new Crusher(
                    new Pos(-40 - i * 5, 49, -88),
                    9,
                    normalEnd,
                    fourthBelt,
                    server
            );
        }
        new ConveyerBelt(
                new Pos(-70, 39, -88),
                new Pos(-65, 39, -71),
                Direction.SOUTH,
                new Pos(-68, 40, -88),
                fourthBelt,
                normalEnd
        );

        return start;
    }

}
