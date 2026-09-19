package net.hollowcube.mapmaker.map.entity.impl.other;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.DyeColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CushionEntityTest {

    static {
        MinecraftServer.init();
    }

    private static final Vec ORIGIN = new Vec(0, 0, 0);

    @Test
    void placesOnTopFaceAtClickHeight() {
        var blocks = blocks(Map.of(ORIGIN, Block.STONE));
        var position = CushionEntity.placementPosition(blocks, new Vec(0.5, 2.6, -1), 10,
            ORIGIN, BlockFace.TOP, new Vec(0.3, 1, 0.7));

        assertNotNull(position);
        assertEquals(0.5, position.x());
        assertEquals(1, position.y());
        assertEquals(0.5, position.z());
        assertEquals(0, position.yaw());
    }

    @Test
    void placesOnBottomSlab() {
        var blocks = blocks(Map.of(ORIGIN, Block.OAK_SLAB));
        var position = CushionEntity.placementPosition(blocks, new Vec(0.5, 2.1, -1), 0,
            ORIGIN, BlockFace.TOP, new Vec(0.5, 0.5, 0.5));

        assertNotNull(position);
        assertEquals(0.5, position.y());
    }

    @Test
    void refusesSideFaces() {
        var blocks = blocks(Map.of(ORIGIN, Block.STONE));
        assertNull(CushionEntity.placementPosition(blocks, new Vec(0.5, 1.6, -1), 0,
            ORIGIN, BlockFace.NORTH, new Vec(0.5, 0.5, 0)));
    }

    @Test
    void retracesAgainstCauldronCollisionShape() {
        var blocks = blocks(Map.of(ORIGIN, Block.CAULDRON));

        // Looking straight down the opening lands on the inner floor
        var floor = CushionEntity.placementPosition(blocks, new Vec(0.5, 2.6, 0.5), 0,
            ORIGIN, BlockFace.TOP, new Vec(0.5, 0.25, 0.5));
        assertNotNull(floor);
        assertEquals(0.25, floor.y());

        // The client reports the top face for a click on the inner wall, the collision shape says otherwise
        assertNull(CushionEntity.placementPosition(blocks, new Vec(-1, 3, 0.5), 0,
            ORIGIN, BlockFace.TOP, new Vec(0.875, 0.6, 0.5)));
    }

    @Test
    void refusesWithoutSupport() {
        var blocks = blocks(Map.of(ORIGIN, Block.STONE));
        var box = CushionEntity.restingBox(new Vec(0.5, 1.2, 0.5));
        assertFalse(CushionEntity.canRest(blocks, box));
        assertTrue(CushionEntity.canRest(blocks, CushionEntity.restingBox(new Vec(0.5, 1, 0.5))));
    }

    @Test
    void refusesInsideBlocks() {
        var inside = CushionEntity.restingBox(new Vec(0.5, 0.5, 0.5));
        assertFalse(CushionEntity.canRest(blocks(Map.of(ORIGIN, Block.STONE)), inside));
        // Glass never smothers, but its collision still fills the resting sliver
        assertFalse(CushionEntity.canRest(blocks(Map.of(ORIGIN, Block.GLASS)), inside));
    }

    @Test
    void persistsColor() {
        var cushion = new CushionEntity(UUID.randomUUID());
        cushion.setColor(DyeColor.LIGHT_GRAY);
        var tag = CompoundBinaryTag.builder();
        cushion.writeData(tag);
        var data = tag.build();
        assertEquals(StringBinaryTag.stringBinaryTag("light_gray"), data.get("color"));

        var loaded = new CushionEntity(UUID.randomUUID());
        loaded.readData(data.remove("NoGravity"));
        assertEquals(DyeColor.LIGHT_GRAY, loaded.color());
        assertTrue(loaded.hasNoGravity());
    }

    @Test
    void hitboxMatchesTheClient() {
        var box = new CushionEntity(UUID.randomUUID()).getBoundingBox();
        assertEquals(1.0, box.width());
        assertEquals(0.25, box.height());
    }

    private static Block.Getter blocks(Map<Vec, Block> blocks) {
        var copy = new HashMap<Point, Block>();
        blocks.forEach((pos, block) -> copy.put(new Vec(pos.blockX(), pos.blockY(), pos.blockZ()), block));
        return (x, y, z, _) -> copy.getOrDefault(new Vec(x, y, z), Block.AIR);
    }
}
