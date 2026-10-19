package net.hollowcube.mapmaker.map.instance;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.hollowcube.mapmaker.map.block.Trigger;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.event.entity.Map2PlayerEnterEntityEvent;
import net.hollowcube.mapmaker.test.AbstractMapWorldTest;
import net.hollowcube.mapmaker.test.TestMapWorld;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTouchingBlocksTest extends AbstractMapWorldTest<TestMapWorld> {
    private static final BlockVec PLATE_POS = new BlockVec(5, 40, 5);
    private static final Pos ON_PLATE = new Pos(5.5, 40, 5.5);
    private static final Pos OFF_PLATE = new Pos(0.5, 40, 0.5);

    private final RecordingPlate plate = new RecordingPlate();

    @Override
    protected @NotNull TestMapWorld createWorld(@NotNull Env env) {
        var world = new TestMapWorld();
        world.instance().loadChunk(0, 0).join();
        return world;
    }

    @Test
    void entersAndExitsIndexedPlate() {
        world.instance().setBlock(PLATE_POS, Block.STONE_PRESSURE_PLATE.withHandler(plate));
        env.tick();
        var player = (MapPlayer) spawnTestPlayer(OFF_PLATE);
        plate.events.clear();

        moveTo(player, ON_PLATE);
        moveTo(player, ON_PLATE.add(0.1, 0, 0));
        moveTo(player, OFF_PLATE);
        assertEquals(List.of("enter " + PLATE_POS, "exit " + PLATE_POS), plate.events);
    }

    @Test
    void silentUpdatesDoNotFire() {
        world.instance().setBlock(PLATE_POS, Block.STONE_PRESSURE_PLATE.withHandler(plate));
        env.tick();
        var player = (MapPlayer) spawnTestPlayer(OFF_PLATE);
        plate.events.clear();

        player.teleport(ON_PLATE).join();
        player.updateTouchingState(world, false);
        moveTo(player, ON_PLATE);
        assertEquals(List.of(), plate.events);
    }

    @Test
    void placedPlateTriggersAfterSafePoint() {
        var player = (MapPlayer) spawnTestPlayer(OFF_PLATE);
        world.instance().setBlock(PLATE_POS, Block.STONE_PRESSURE_PLATE.withHandler(plate));

        moveTo(player, ON_PLATE);
        assertEquals(List.of(), plate.events);

        env.tick();
        moveTo(player, ON_PLATE);
        assertEquals(List.of("enter " + PLATE_POS), plate.events);
    }

    @Test
    void removedPlateExitsBeforeSafePoint() {
        world.instance().setBlock(PLATE_POS, Block.STONE_PRESSURE_PLATE.withHandler(plate));
        env.tick();
        var player = (MapPlayer) spawnTestPlayer(ON_PLATE);
        moveTo(player, ON_PLATE);
        plate.events.clear();

        world.instance().setBlock(PLATE_POS, Block.AIR);
        moveTo(player, ON_PLATE);
        assertEquals(List.of("exit " + PLATE_POS), plate.events);
    }

    @Test
    void touchesReuseTheIndexedTrigger() {
        world.instance().setBlock(PLATE_POS, Block.STONE_PRESSURE_PLATE.withHandler(plate));
        env.tick();
        var player = (MapPlayer) spawnTestPlayer(OFF_PLATE);
        int created = plate.triggersCreated;

        moveTo(player, ON_PLATE);
        moveTo(player, OFF_PLATE);
        moveTo(player, ON_PLATE);
        assertEquals(created, plate.triggersCreated);
    }

    @Test
    void changedDataIsLiveBeforeSafePoint() {
        world.instance().setBlock(PLATE_POS, Block.STONE_PRESSURE_PLATE.withHandler(plate)
            .withNbt(CompoundBinaryTag.builder().putString("data", "old").build()));
        env.tick();
        var player = (MapPlayer) spawnTestPlayer(OFF_PLATE);
        plate.events.clear();

        world.instance().setBlock(PLATE_POS, Block.STONE_PRESSURE_PLATE.withHandler(plate)
            .withNbt(CompoundBinaryTag.builder().putString("data", "new").build()));
        moveTo(player, ON_PLATE);
        moveTo(player, OFF_PLATE);
        assertEquals(List.of("enter " + PLATE_POS + " new", "exit " + PLATE_POS + " new"), plate.events);
    }

    @Test
    void plateTeleportEntersMarkerInSameUpdate() {
        // A plate that teleports into a marker region enters that marker in the same update; maps depend on it.
        var markerPos = new Pos(10.5, 40, 10.5);
        var marker = new MarkerEntity();
        marker.setRegion(new Vec(-1, 0, -1), new Vec(1, 1, 1));
        marker.setInstance(world.instance(), markerPos).join();
        world.queueCollisionTreeRebuild();
        world.instance().setBlock(PLATE_POS, Block.STONE_PRESSURE_PLATE.withHandler(new TeleportPlate(markerPos)));
        env.tick();

        var player = (MapPlayer) spawnTestPlayer(OFF_PLATE);
        var entered = new ArrayList<Entity>();
        world.eventNode().addListener(Map2PlayerEnterEntityEvent.class, event -> entered.add(event.enteredEntity()));

        moveTo(player, ON_PLATE);
        assertEquals(List.of(marker), entered);
    }

    @Test
    void teleportOutOfWaterClearsInWater() {
        world.instance().setBlock(PLATE_POS, Block.WATER);
        world.instance().setBlock(PLATE_POS.add(0, 1, 0), Block.WATER);
        var player = (MapPlayer) spawnTestPlayer(ON_PLATE);
        env.tick();
        assertTrue(player.isInWater());

        player.teleport(OFF_PLATE).join();
        assertFalse(player.isInWater());
    }

    private record TeleportPlate(Pos destination) implements CollidableBlock {
        @Override
        public Key getKey() {
            return Key.key("test:teleport_plate");
        }

        @Override
        public BoundingBox collisionBox() {
            return new BoundingBox(14.0 / 16.0, 4.0 / 16.0, 14.0 / 16.0);
        }

        @Override
        public Trigger createTrigger(BlockVec position, Block block) {
            return (_, player) -> player.teleport(destination).join();
        }
    }

    private void moveTo(MapPlayer player, Pos pos) {
        player.teleport(pos).join();
        player.updateTouchingState(world, true);
    }

    private static final class RecordingPlate implements CollidableBlock {
        final List<String> events = new ArrayList<>();
        int triggersCreated = 0;

        @Override
        public Key getKey() {
            return Key.key("test:recording_plate");
        }

        @Override
        public BoundingBox collisionBox() {
            return new BoundingBox(14.0 / 16.0, 4.0 / 16.0, 14.0 / 16.0);
        }

        @Override
        public Trigger createTrigger(BlockVec position, Block block) {
            triggersCreated++;
            var nbt = block.nbt();
            var label = nbt == null || !nbt.keySet().contains("data") ? "" : " " + nbt.getString("data");
            return new Trigger() {
                @Override
                public void onEnter(MapWorld world, Player player) {
                    events.add("enter " + position + label);
                }

                @Override
                public void onExit(MapWorld world, Player player) {
                    events.add("exit " + position + label);
                }
            };
        }
    }
}
