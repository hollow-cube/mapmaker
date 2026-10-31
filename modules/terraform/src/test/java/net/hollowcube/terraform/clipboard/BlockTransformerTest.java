package net.hollowcube.terraform.clipboard;

import net.hollowcube.terraform.util.Transform;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class BlockTransformerTest {

    @ParameterizedTest
    @CsvSource({"north,east", "east,south", "south,west", "west,north"})
    void facingRotatesClockwise(String from, String to) {
        Block stairs = Block.OAK_STAIRS.withProperties(
            Map.of("facing", from, "half", "bottom", "shape", "straight")
        );
        assertEquals(to, transform(stairs, Transform.rotateY(90)).getProperty("facing"));
    }

    @Test
    void verticalFlipSwapsDoorAndPlantHalves() {
        for (Block type : new Block[] {Block.OAK_DOOR, Block.TALL_GRASS}) {
            var lower = type.withProperty("half", "lower");
            var upper = transform(lower, Transform.mirror(Transform.Axis.Y));
            assertEquals("upper", upper.getProperty("half"));
            assertEquals(lower, transform(upper, Transform.mirror(Transform.Axis.Y)));
        }
    }

    @Test
    void hopperFacingSnapsToALegalDirection() {
        var result = transform(
            Block.HOPPER.withProperty("facing", "down"),
            Transform.mirror(Transform.Axis.Y)
        );
        String facing = result.getProperty("facing");
        assertTrue(
            Block.HOPPER.possibleStates()
                .stream()
                .anyMatch(s -> s.getProperty("facing").equals(facing)),
            facing
        );
    }

    @Test
    void facingMirrorsAcrossX() {
        Block stairs = Block.OAK_STAIRS.withProperty("facing", "east");
        assertEquals(
            "west",
            transform(stairs, Transform.mirror(Transform.Axis.X)).getProperty("facing")
        );
    }

    @Test
    void facingFourRotationsRestores() {
        Block stairs = Block.OAK_STAIRS.withProperty("facing", "east");
        Block out = stairs;
        for (int i = 0; i < 4; i++) out = transform(out, Transform.rotateY(90));
        assertEquals("east", out.getProperty("facing"));
    }

    @Test
    void logAxisFolds() {
        Block log = Block.OAK_LOG.withProperty("axis", "x");
        assertEquals("z", transform(log, Transform.rotateY(90)).getProperty("axis"));
        assertEquals(
            "y",
            transform(
                Block.OAK_LOG.withProperty("axis", "y"),
                Transform.rotateY(90)
            ).getProperty("axis")
        );
    }

    @Test
    void stairsHalfSwapsOnVerticalFlip() {
        Block top = Block.OAK_STAIRS.withProperty("half", "top");
        assertEquals(
            "bottom",
            transform(top, Transform.mirror(Transform.Axis.Y)).getProperty("half")
        );
        assertEquals("top", transform(top, Transform.rotateY(90)).getProperty("half"));
    }

    @Test
    void slabTypeSwapsOnVerticalFlip() {
        Block bottom = Block.OAK_SLAB.withProperty("type", "bottom");
        assertEquals(
            "top",
            transform(bottom, Transform.mirror(Transform.Axis.Y)).getProperty("type")
        );
        Block dbl = Block.OAK_SLAB.withProperty("type", "double");
        assertEquals(
            "double",
            transform(dbl, Transform.mirror(Transform.Axis.Y)).getProperty("type")
        );
    }

    @Test
    void stairShapeSwapsLeftRightOnHorizontalFlip() {
        Block inner = Block.OAK_STAIRS.withProperties(
            Map.of("facing", "north", "shape", "inner_left")
        );
        assertEquals(
            "inner_right",
            transform(inner, Transform.mirror(Transform.Axis.X)).getProperty("shape")
        );
        // A rotation turns the corner through facing.
        assertEquals("inner_left", transform(inner, Transform.rotateY(90)).getProperty("shape"));
    }

    @Test
    void doorHingeSwapsOnHorizontalFlip() {
        Block door = Block.OAK_DOOR.withProperties(
            Map.of("facing", "north", "hinge", "left", "half", "lower")
        );
        Block flipped = transform(door, Transform.mirror(Transform.Axis.X));
        assertEquals("right", flipped.getProperty("hinge"));
        assertEquals("lower", flipped.getProperty("half"));
    }

    @Test
    void fenceSidesRotateWithoutClobber() {
        Block fence = Block.OAK_FENCE.withProperties(
            Map.of("north", "true", "east", "false", "south", "false", "west", "false")
        );
        Block out = transform(fence, Transform.rotateY(90));
        assertEquals("true", out.getProperty("east"));
        assertEquals("false", out.getProperty("north"), "old north cleared, not left set");
        assertEquals("false", out.getProperty("south"));
        assertEquals("false", out.getProperty("west"));
    }

    @Test
    void fenceFourRotationsRestores() {
        Block fence = Block.OAK_FENCE.withProperties(
            Map.of("north", "true", "east", "true", "south", "false", "west", "false")
        );
        Block out = fence;
        for (int i = 0; i < 4; i++) out = transform(out, Transform.rotateY(90));
        assertEquals("true", out.getProperty("north"));
        assertEquals("true", out.getProperty("east"));
        assertEquals("false", out.getProperty("south"));
        assertEquals("false", out.getProperty("west"));
    }

    @Test
    void railShapeTransforms() {
        Block ns = Block.RAIL.withProperty("shape", "north_south");
        assertEquals("east_west", transform(ns, Transform.rotateY(90)).getProperty("shape"));
        Block curve = Block.RAIL.withProperty("shape", "north_east");
        assertEquals("south_east", transform(curve, Transform.rotateY(90)).getProperty("shape"));
        Block asc = Block.RAIL.withProperty("shape", "ascending_north");
        assertEquals("ascending_east", transform(asc, Transform.rotateY(90)).getProperty("shape"));
    }

    @Test
    void signRotationTransforms() {
        Block sign = Block.OAK_SIGN.withProperty("rotation", "0");
        assertEquals("4", transform(sign, Transform.rotateY(90)).getProperty("rotation"));
    }

    @Test
    void crafterOrientationRotates() {
        Block crafter = Block.CRAFTER.withProperty("orientation", "north_up");
        assertEquals(
            "east_up",
            transform(crafter, Transform.rotateY(90)).getProperty("orientation")
        );
    }

    @Test
    void jigsawOrientationRotatesHorizontalTop() {
        Block jigsaw = Block.JIGSAW.withProperty("orientation", "down_north");
        assertEquals(
            "down_east",
            transform(jigsaw, Transform.rotateY(90)).getProperty("orientation")
        );
    }

    @Test
    void crafterOrientationMirrors() {
        Block crafter = Block.CRAFTER.withProperty("orientation", "east_up");
        assertEquals(
            "west_up",
            transform(crafter, Transform.mirror(Transform.Axis.X)).getProperty("orientation")
        );
    }

    @Test
    void buttonFaceSwapsOnVerticalFlip() {
        Block button = Block.STONE_BUTTON.withProperties(
            Map.of("face", "floor", "facing", "north")
        );
        assertEquals(
            "ceiling",
            transform(button, Transform.mirror(Transform.Axis.Y)).getProperty("face")
        );
        assertEquals("floor", transform(button, Transform.rotateY(90)).getProperty("face"));
    }

    @Test
    void bellAttachmentSwapsOnVerticalFlip() {
        Block bell = Block.BELL.withProperties(Map.of("attachment", "floor", "facing", "north"));
        assertEquals(
            "ceiling",
            transform(bell, Transform.mirror(Transform.Axis.Y)).getProperty("attachment")
        );
    }

    @Test
    void plainBlockUnchanged() {
        assertEquals(Block.STONE, transform(Block.STONE, Transform.rotateY(90)));
    }

    @Test
    void nbtAndHandlerPreserved() {
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putByte("Foo", (byte) 7).build();
        BlockHandler handler = BlockHandler.Dummy.get("test:sign");
        Block sign = Block.OAK_SIGN.withProperty("rotation", "0").withNbt(nbt).withHandler(handler);
        Block out = transform(sign, Transform.rotateY(90));
        assertEquals("4", out.getProperty("rotation"));
        assertEquals((byte) 7, Objects.requireNonNull(out.nbt()).getByte("Foo"));
        assertSame(handler, out.handler());
    }

    @Test
    void cellsSharingAStateKeepTheirOwnNbt() {
        BlockTransformer bt = new BlockTransformer(Transform.rotateY(90));
        Block base = Block.OAK_SIGN.withProperty("rotation", "0");
        Block a = bt.transform(
            base.withNbt(CompoundBinaryTag.builder().putString("Text", "a").build())
        );
        Block b = bt.transform(
            base.withNbt(CompoundBinaryTag.builder().putString("Text", "b").build())
        );
        Block plain = bt.transform(base);
        assertEquals("a", Objects.requireNonNull(a.nbt()).getString("Text"));
        assertEquals("b", Objects.requireNonNull(b.nbt()).getString("Text"));
        assertNull(plain.nbt());
        assertEquals(a.stateId(), b.stateId());
        assertEquals(a.stateId(), plain.stateId());
    }

    @Test
    void translationAloneIsANoop() {
        Block stairs = Block.OAK_STAIRS.withProperty("facing", "east");
        assertEquals(stairs, transform(stairs, Transform.translate(4, -2, 9)));
    }

    @Test
    void identityIsANoop() {
        Block stairs = Block.OAK_STAIRS.withProperty("facing", "east");
        assertEquals(stairs, transform(stairs, Transform.identity()));
    }

    @Test
    void chestTypeSwapsOnHorizontalFlipOnly() {
        Block left = Block.CHEST.withProperties(Map.of("facing", "north", "type", "left"));
        assertEquals(
            "right",
            transform(left, Transform.mirror(Transform.Axis.X)).getProperty("type")
        );
        assertEquals("left", transform(left, Transform.rotateY(90)).getProperty("type"));
        assertEquals(
            "left",
            transform(left, Transform.mirror(Transform.Axis.Y)).getProperty("type")
        );
    }

    @Test
    void xRotationTurnsAPistonDown() {
        Block piston = Block.PISTON.withProperty("facing", "north");
        assertEquals("down", transform(piston, Transform.rotateX(90)).getProperty("facing"));
    }

    @Test
    void transformStateMatchesTransformAndIsMemoized() {
        BlockTransformer bt = new BlockTransformer(Transform.rotateY(90));
        int east = Block.OAK_STAIRS.withProperty("facing", "east").stateId();
        int south = Block.OAK_STAIRS.withProperty("facing", "south").stateId();
        assertEquals(south, bt.transformState(east));
        assertEquals(south, bt.transformState(east));
        assertEquals(south, bt.transform(Block.fromStateId(east)).stateId());
    }

    private static Block transform(Block block, Transform t) {
        return new BlockTransformer(t).transform(block);
    }

    @Test
    void identitySnapsEveryDirectionToItself() {
        for (Direction d : Direction.values()) {
            assertEquals(d, BlockTransformer.snapOmni(d, Transform.identity()));
        }
    }

    @Test
    void candidatesRestrictTheSnap() {
        Direction[] southWest = {Direction.SOUTH, Direction.WEST};
        assertEquals(
            Direction.SOUTH,
            BlockTransformer.snap(Direction.NORTH, Transform.rotateY(60), southWest)
        );
    }

    @Test
    void offAxisRotationSnapsToTheNearestDirection() {
        assertEquals(
            Direction.EAST,
            BlockTransformer.snapOmni(Direction.NORTH, Transform.rotateY(60))
        );
        assertEquals(
            Direction.NORTH,
            BlockTransformer.snapOmni(Direction.NORTH, Transform.rotateY(30))
        );
    }

    @Test
    void horizontalSnapNeverPicksVertical() {
        // Mostly down, but only the four horizontals are candidates.
        Transform tilt = Transform.rotateX(80);
        assertEquals(Direction.DOWN, BlockTransformer.snapOmni(Direction.NORTH, tilt));
        assertEquals(Direction.NORTH, BlockTransformer.snapHorizontal(Direction.NORTH, tilt));
    }

    @Test
    void rotateYTurnsFacingClockwiseFromAbove() {
        Transform r = Transform.rotateY(90);
        assertEquals(Direction.EAST, BlockTransformer.snapOmni(Direction.NORTH, r));
        assertEquals(Direction.SOUTH, BlockTransformer.snapOmni(Direction.EAST, r));
        assertEquals(Direction.WEST, BlockTransformer.snapOmni(Direction.SOUTH, r));
        assertEquals(Direction.NORTH, BlockTransformer.snapOmni(Direction.WEST, r));
        assertEquals(Direction.UP, BlockTransformer.snapOmni(Direction.UP, r));
    }

    @Test
    void mirrorReflectsFacing() {
        Transform mx = Transform.mirror(Transform.Axis.X);
        assertEquals(Direction.WEST, BlockTransformer.snapOmni(Direction.EAST, mx));
        assertEquals(Direction.NORTH, BlockTransformer.snapOmni(Direction.NORTH, mx));
    }

    @Test
    void axisFoldsUnderRotation() {
        Transform r = Transform.rotateY(90);
        assertEquals("z", BlockTransformer.axis("x", r));
        assertEquals("x", BlockTransformer.axis("z", r));
        assertEquals("y", BlockTransformer.axis("y", r));
        assertEquals("x", BlockTransformer.axis("y", Transform.rotateZ(90)));
    }

    @Test
    void rotationCompassRotates() {
        Transform r = Transform.rotateY(90);
        // 0 = south, 4 = west.
        assertEquals(4, BlockTransformer.rotation(0, r));
        assertEquals(8, BlockTransformer.rotation(4, r));
        assertEquals(12, BlockTransformer.rotation(8, r));
        assertEquals(0, BlockTransformer.rotation(12, r));
        assertEquals(7, BlockTransformer.rotation(3, r));
        assertEquals(4, BlockTransformer.rotation(16, r), "index wraps modulo 16");
    }

    @Test
    void rotationCompassMirrors() {
        Transform mx = Transform.mirror(Transform.Axis.X);
        assertEquals(0, BlockTransformer.rotation(0, mx));
        assertEquals(12, BlockTransformer.rotation(4, mx));
    }

    @Test
    void rotationCompassFullTurnIsIdentity() {
        Transform r = Transform.rotateY(90);
        for (int start = 0; start < 16; start++) {
            int v = start;
            for (int i = 0; i < 4; i++) v = BlockTransformer.rotation(v, r);
            assertEquals(start, v, "rotation index " + start);
        }
    }

    @Test
    void rotationCompassIgnoresAVerticalFlip() {
        Transform my = Transform.mirror(Transform.Axis.Y);
        for (int i = 0; i < 16; i++) assertEquals(i, BlockTransformer.rotation(i, my));
    }
}
