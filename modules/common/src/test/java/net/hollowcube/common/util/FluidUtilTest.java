package net.hollowcube.common.util;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidUtilTest {
    private static final double SOURCE_HEIGHT = 8 / 9.0;

    private static final BoundingBox PLAYER_BOX = new BoundingBox(0.6, 1.8, 0.6);
    private static final BoundingBox CUBE_BOX = new BoundingBox(0.98, 0.98, 0.98);

    private static Block.Getter world(Map<Vec, Block> blocks) {
        return (x, y, z, condition) -> blocks.getOrDefault(new Vec(x, y, z), Block.AIR);
    }

    @Test
    void airOnly() {
        var result = FluidUtil.scan(world(Map.of()), PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertFalse(result.inWater());
        assertFalse(result.inLava());
        assertFalse(result.inPowderSnow());
    }

    @Test
    void waterSourceAtFeet() {
        var getter = world(Map.of(new Vec(0, 40, 0), Block.WATER));
        var result = FluidUtil.scan(getter, PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertTrue(result.inWater());
        assertFalse(result.inLava());
        // A lone source block is 8/9 full, so the surface sits at y = 40 + 8/9.
        assertEquals(SOURCE_HEIGHT, result.waterHeight(), 1e-9);
    }

    @Test
    void waterColumnIsFullHeight() {
        var getter = world(Map.of(
            new Vec(0, 40, 0), Block.WATER,
            new Vec(0, 41, 0), Block.WATER));
        var result = FluidUtil.scan(getter, PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertTrue(result.inWater());
        // The bottom block is full (same fluid above); the top block adds its own 8/9.
        assertEquals(1 + SOURCE_HEIGHT, result.waterHeight(), 1e-9);
    }

    @Test
    void feetAboveSurfaceIsNotInWater() {
        var getter = world(Map.of(new Vec(0, 40, 0), Block.WATER));
        var result = FluidUtil.scan(getter, PLAYER_BOX, new Pos(0.5, 40.95, 0.5));

        assertFalse(result.inWater());
    }

    @Test
    void flowingWaterUsesLevelProperty() {
        var getter = world(Map.of(new Vec(0, 40, 0), Block.WATER.withProperty("level", "4")));
        var result = FluidUtil.scan(getter, PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertTrue(result.inWater());
        assertEquals(4 / 9.0, result.waterHeight(), 1e-9);
    }

    @Test
    void waterloggedBlockCountsAsWater() {
        var getter = world(Map.of(new Vec(0, 40, 0), Block.OAK_STAIRS.withProperty("waterlogged", "true")));
        var result = FluidUtil.scan(getter, PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertTrue(result.inWater());
        assertEquals(SOURCE_HEIGHT, result.waterHeight(), 1e-9);
    }

    @Test
    void lavaSourceAtFeet() {
        var getter = world(Map.of(new Vec(0, 40, 0), Block.LAVA));
        var result = FluidUtil.scan(getter, PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertTrue(result.inLava());
        assertFalse(result.inWater());
        assertEquals(SOURCE_HEIGHT, result.lavaHeight(), 1e-9);
    }

    @Test
    void waterAndLavaAreTrackedSeparately() {
        var blocks = new HashMap<Vec, Block>();
        blocks.put(new Vec(0, 40, 0), Block.WATER);
        blocks.put(new Vec(0, 41, 0), Block.LAVA);
        var result = FluidUtil.scan(world(blocks), PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertTrue(result.inWater());
        assertTrue(result.inLava());
        assertEquals(SOURCE_HEIGHT, result.waterHeight(), 1e-9);
        assertEquals(1 + SOURCE_HEIGHT, result.lavaHeight(), 1e-9);
    }

    @Test
    void powderSnowIsDetected() {
        var getter = world(Map.of(new Vec(0, 40, 0), Block.POWDER_SNOW));
        var result = FluidUtil.scan(getter, PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertTrue(result.inPowderSnow());
        assertFalse(result.inWater());
        assertFalse(result.inLava());
    }

    @Test
    void neighboringColumnWithinBoundingBoxCounts() {
        // The cube is nearly a full block wide, so standing at x = 0.9 overlaps the x = 1 column.
        var getter = world(Map.of(new Vec(1, 40, 0), Block.WATER));
        var result = FluidUtil.scan(getter, CUBE_BOX, new Pos(0.9, 40, 0.5));

        assertTrue(result.inWater());
    }

    @Test
    void fluidOutsideBoundingBoxIsIgnored() {
        var getter = world(Map.of(new Vec(3, 40, 0), Block.WATER));
        var result = FluidUtil.scan(getter, PLAYER_BOX, new Pos(0.5, 40, 0.5));

        assertFalse(result.inWater());
    }
}
