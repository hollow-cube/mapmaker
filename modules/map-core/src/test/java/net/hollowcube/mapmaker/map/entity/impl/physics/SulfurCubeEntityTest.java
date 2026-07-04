package net.hollowcube.mapmaker.map.entity.impl.physics;

import net.hollowcube.mapmaker.map.entity.PhysicsTestEnv;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.stream.Stream;

import static net.hollowcube.mapmaker.map.entity.PhysicsTestEnv.FLOOR_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SulfurCubeEntityTest {

    // Shared knockback-test geometry: the cube at CUBE_SPAWN, the attacker standing due west
    // of it at vanilla player eye height.
    private static final Pos CUBE_SPAWN = new Pos(0.5, FLOOR_Y, 0.5);
    private static final Vec ATTACKER_FEET = new Vec(-1.5, FLOOR_Y, 0.5);
    private static final Vec ATTACKER_EYE = ATTACKER_FEET.add(0, 1.62, 0);

    @BeforeAll
    static void init() {
        PhysicsTestEnv.init();
    }

    /** Normalized look direction from the attacker's eye toward the given height on the cube. */
    private static Vec lookAt(double y) {
        return new Vec(CUBE_SPAWN.x(), y, CUBE_SPAWN.z()).sub(ATTACKER_EYE).normalize();
    }

    private static SulfurCubeEntity cube() {
        return new SulfurCubeEntity(UUID.randomUUID());
    }

    /// Sets the body block and applies the archetype derived from its material, i.e. the combined
    /// behavior before setBodyBlock/setArchetype were split.
    private static void setBody(SulfurCubeEntity cube, ItemStack item) {
        cube.setBodyBlock(item);
        cube.setArchetype(SulfurCubeEntity.archetypeFor(item.material()));
    }

    private static SulfurCubeEntity spawnCube(Instance instance, Pos pos, Material bodyBlock) {
        var cube = cube();
        setBody(cube, ItemStack.of(bodyBlock));
        cube.setInstance(instance, pos).join();
        return cube;
    }

    private static void tick(SulfurCubeEntity cube, int count) {
        for (int i = 0; i < count; i++) cube.tick(50L * i);
    }

    // Size

    @Test
    void adultSizeAndVanillaBoundingBox() {
        var cube = cube();
        assertEquals(2, cube.getEntityMeta().getSize());
        // 0.49 x size, not the 0.51 slime formula.
        assertEquals(0.98, cube.getBoundingBox().width(), 1e-9);
        assertEquals(0.98, cube.getBoundingBox().height(), 1e-9);
        assertEquals(0.98, cube.getBoundingBox().depth(), 1e-9);
    }

    // Archetype resolution

    @ParameterizedTest
    @MethodSource("archetypeExpectations")
    void archetypeAttributesMatchVanillaData(Material item, double kbRes, double bounciness,
                                             double friction, double drag) {
        var cube = cube();
        setBody(cube, ItemStack.of(item));

        assertTrue(cube.hasBodyBlock());
        assertEquals(kbRes, cube.getAttribute(Attribute.KNOCKBACK_RESISTANCE), 1e-6);
        assertEquals(bounciness, cube.getAttribute(Attribute.BOUNCINESS), 1e-6);
        assertEquals(friction, cube.getAttribute(Attribute.FRICTION_MODIFIER), 1e-6);
        assertEquals(drag, cube.getAttribute(Attribute.AIR_DRAG_MODIFIER), 1e-6);
    }

    static Stream<Arguments> archetypeExpectations() {
        // Expected values are the vanilla sulfur_cube_archetype registry data.
        return Stream.of(
            arguments(Material.DIRT, -1.0, 0.5, 0.3, 0.1),           // regular
            arguments(Material.OAK_PLANKS, -2.0, 0.9, 0.3, 0.01),    // bouncy
            arguments(Material.WHITE_WOOL, -1.0, 1.0, 0.3, 1.8),     // light
            arguments(Material.PACKED_ICE, 0.5, 0.1, 0.05, 0.01),    // fast_sliding
            arguments(Material.HONEYCOMB_BLOCK, -2.0, 0.0, 2.0, 0.01), // sticky
            arguments(Material.TNT, -1.0, 0.5, 0.3, 0.3));           // explosive: movement values only
    }

    @Test
    void explosionKnockbackResistanceClampsToAttributeRange() {
        var cube = cube();
        setBody(cube, ItemStack.of(Material.DIRT));
        // The regular archetype carries a -1 modifier, but the attribute's range is [0, 1]
        // (unlike knockback_resistance's [-2, 1]), so it clamps to 0 - same as vanilla's
        // AttributeInstance sanitization.
        assertEquals(0.0, cube.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE), 1e-6);
    }

    @Test
    void nonArchetypeItemLeavesDefaults() {
        var cube = cube();
        setBody(cube, ItemStack.of(Material.STICK));

        assertTrue(cube.hasBodyBlock());
        assertEquals(Attribute.KNOCKBACK_RESISTANCE.defaultValue(), cube.getAttribute(Attribute.KNOCKBACK_RESISTANCE));
        assertEquals(Attribute.BOUNCINESS.defaultValue(), cube.getAttribute(Attribute.BOUNCINESS));
        assertEquals(Attribute.FRICTION_MODIFIER.defaultValue(), cube.getAttribute(Attribute.FRICTION_MODIFIER));
    }

    @Test
    void clearingBodyBlockResetsAttributes() {
        var cube = cube();
        setBody(cube, ItemStack.of(Material.HONEYCOMB_BLOCK));
        setBody(cube, ItemStack.AIR);

        assertFalse(cube.hasBodyBlock());
        assertTrue(cube.getEquipment(EquipmentSlot.BODY).isAir());
        assertEquals(Attribute.KNOCKBACK_RESISTANCE.defaultValue(), cube.getAttribute(Attribute.KNOCKBACK_RESISTANCE));
        assertEquals(Attribute.BOUNCINESS.defaultValue(), cube.getAttribute(Attribute.BOUNCINESS));
        assertEquals(Attribute.FRICTION_MODIFIER.defaultValue(), cube.getAttribute(Attribute.FRICTION_MODIFIER));
        assertEquals(Attribute.AIR_DRAG_MODIFIER.defaultValue(), cube.getAttribute(Attribute.AIR_DRAG_MODIFIER));
    }

    @Test
    void switchingArchetypesReplacesValues() {
        var cube = cube();
        setBody(cube, ItemStack.of(Material.HONEYCOMB_BLOCK));
        setBody(cube, ItemStack.of(Material.WHITE_WOOL));

        assertEquals(1.0, cube.getAttribute(Attribute.BOUNCINESS), 1e-6);
        assertEquals(0.3, cube.getAttribute(Attribute.FRICTION_MODIFIER), 1e-6);
        assertEquals(1.8, cube.getAttribute(Attribute.AIR_DRAG_MODIFIER), 1e-6);
    }

    // Persistence

    @Test
    void bodyBlockAndAttributesRoundTrip() {
        var original = cube();
        setBody(original, ItemStack.of(Material.DIRT));

        var builder = CompoundBinaryTag.builder();
        original.writeData(builder);

        var loaded = cube();
        loaded.readData(builder.build());

        assertTrue(loaded.hasBodyBlock());
        assertEquals(Material.DIRT, loaded.getEquipment(EquipmentSlot.BODY).material());
        assertEquals(-1.0, loaded.getAttribute(Attribute.KNOCKBACK_RESISTANCE), 1e-6);
        assertEquals(0.5, loaded.getAttribute(Attribute.BOUNCINESS), 1e-6);
        assertEquals(0.3, loaded.getAttribute(Attribute.FRICTION_MODIFIER), 1e-6);
        assertEquals(0.1, loaded.getAttribute(Attribute.AIR_DRAG_MODIFIER), 1e-6);
    }

    // Hit knockback

    @Test
    void hitKnockbackPushesAwayFromAttackerAndUp() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var cube = spawnCube(instance, CUBE_SPAWN, Material.DIRT);

        cube.applyHitKnockback(ATTACKER_FEET, ATTACKER_EYE, lookAt(FLOOR_Y + 0.49), 1.0f);

        assertTrue(cube.getVelocity().x() > 0, "not pushed away: " + cube.getVelocity());
        assertEquals(0.0, cube.getVelocity().z(), 1e-6);
        assertTrue(cube.getVelocity().y() > 0, "no vertical pop: " + cube.getVelocity());
    }

    @Test
    void hitKnockbackScalesWithSqrtOfDamage() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var weak = spawnCube(instance, CUBE_SPAWN, Material.DIRT);
        var strong = spawnCube(instance, CUBE_SPAWN, Material.DIRT);

        var look = lookAt(FLOOR_Y + 0.49);
        weak.applyHitKnockback(ATTACKER_FEET, ATTACKER_EYE, look, 1.0f);
        strong.applyHitKnockback(ATTACKER_FEET, ATTACKER_EYE, look, 4.0f);

        assertEquals(2.0, strong.getVelocity().x() / weak.getVelocity().x(), 1e-6);
        assertEquals(2.0, strong.getVelocity().y() / weak.getVelocity().y(), 1e-6);
    }

    @Test
    void negativeKnockbackResistanceAmplifiesHits() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var amplified = spawnCube(instance, CUBE_SPAWN, Material.DIRT); // kbRes -1
        var neutral = spawnCube(instance, CUBE_SPAWN, Material.DIRT);
        neutral.setAttribute(Attribute.KNOCKBACK_RESISTANCE, 0.0);

        var look = lookAt(FLOOR_Y + 0.49);
        amplified.applyHitKnockback(ATTACKER_FEET, ATTACKER_EYE, look, 1.0f);
        neutral.applyHitKnockback(ATTACKER_FEET, ATTACKER_EYE, look, 1.0f);

        assertEquals(2.0, amplified.getVelocity().x() / neutral.getVelocity().x(), 1e-6);
    }

    @Test
    void aimingAtBottomPopsHigherThanAimingAtTop() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var poppedUp = spawnCube(instance, CUBE_SPAWN, Material.DIRT);
        var slammedFlat = spawnCube(instance, CUBE_SPAWN, Material.DIRT);

        poppedUp.applyHitKnockback(ATTACKER_FEET, ATTACKER_EYE, lookAt(FLOOR_Y), 1.0f);
        slammedFlat.applyHitKnockback(ATTACKER_FEET, ATTACKER_EYE, lookAt(FLOOR_Y + 0.98), 1.0f);

        assertTrue(poppedUp.getVelocity().y() > slammedFlat.getVelocity().y(),
            "bottom=" + poppedUp.getVelocity().y() + " top=" + slammedFlat.getVelocity().y());
        assertTrue(poppedUp.getVelocity().x() < slammedFlat.getVelocity().x(),
            "vertical pop should trade away horizontal power");
    }

    // Tick behavior

    @Test
    void yawSnapsToNearestRightAngle() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var cube = spawnCube(instance, CUBE_SPAWN, Material.DIRT);

        cube.setView(37f, 0f);
        cube.tick(0);
        assertEquals(0f, cube.getPosition().yaw());

        cube.setView(50f, 0f);
        cube.tick(50);
        assertEquals(90f, cube.getPosition().yaw());

        cube.setView(-134f, 0f);
        cube.tick(100);
        assertEquals(-90f, cube.getPosition().yaw());
    }

    @Test
    void inertWithoutBodyBlockFallsAndRests() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var cube = cube();
        cube.setInstance(instance, new Pos(0.5, FLOOR_Y + 5, 0.5)).join();

        tick(cube, 100);
        assertEquals(FLOOR_Y, cube.getPosition().y(), 1e-4);
    }

    // Buoyancy

    private static Instance createWaterPool() {
        var instance = PhysicsTestEnv.createFlatInstance();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = FLOOR_Y; y <= FLOOR_Y + 4; y++) {
                    instance.setBlock(x, y, z, Block.WATER);
                }
            }
        }
        return instance;
    }

    @Test
    void buoyantCubeFloatsToTheSurface() {
        var instance = createWaterPool();
        var cube = spawnCube(instance, new Pos(0.5, FLOOR_Y + 1, 0.5), Material.DIRT); // regular: buoyant

        tick(cube, 300);
        // Water surface is at FLOOR_Y + 4 + 8/9; the cube should bob just below it.
        assertTrue(cube.getPosition().y() > FLOOR_Y + 3.5,
            "did not float up: " + cube.getPosition().y());
    }

    @Test
    void nonBuoyantCubeSinks() {
        var instance = createWaterPool();
        var cube = spawnCube(instance, new Pos(0.5, FLOOR_Y + 3, 0.5), Material.PACKED_ICE); // fast_sliding: not buoyant

        tick(cube, 300);
        assertTrue(cube.getPosition().y() < FLOOR_Y + 1,
            "did not sink: " + cube.getPosition().y());
    }

    @Test
    void cubeSinksSlowerThanItFallsInAir() {
        var instance = createWaterPool();
        var inWater = spawnCube(instance, new Pos(0.5, FLOOR_Y + 3, 0.5), Material.PACKED_ICE);

        var airInstance = PhysicsTestEnv.createFlatInstance();
        var inAir = spawnCube(airInstance, new Pos(0.5, FLOOR_Y + 3, 0.5), Material.PACKED_ICE);

        tick(inWater, 10);
        tick(inAir, 10);

        assertTrue(inWater.getPosition().y() > inAir.getPosition().y() + 0.5,
            "water=" + inWater.getPosition().y() + " air=" + inAir.getPosition().y());
    }

    // Player push

    private static final class FakePlayerConnection extends PlayerConnection {
        @Override
        public void sendPacket(@NotNull SendablePacket packet) {
        }

        @Override
        public @NotNull SocketAddress getRemoteAddress() {
            return new InetSocketAddress("localhost", 25565);
        }

        @Override
        public void disconnect() {
        }
    }

    @Test
    void walkingPlayerPushesTheCubeOnlyAsViewer() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var cube = spawnCube(instance, CUBE_SPAWN, Material.DIRT);
        cube.setAutoViewable(false); // viewership is managed by hand below
        tick(cube, 5); // settle onto the floor

        var player = new Player(new FakePlayerConnection(), new GameProfile(UUID.randomUUID(), "TestPlayer"));
        player.setInstance(instance, new Pos(1.5, FLOOR_Y, 0.5)).join();

        // Walk toward the cube. Not a viewer yet, so nothing may happen.
        player.refreshPosition(new Pos(1.3, FLOOR_Y, 0.5));
        cube.tick(0);
        assertEquals(0.0, cube.getVelocity().x(), 1e-9);
        assertEquals(0.0, cube.getVelocity().z(), 1e-9);
        // At rest the gravity cycle keeps a small constant downward velocity; the push's
        // vertical pop is measured against it.
        final double restingY = cube.getVelocity().y();

        // As a viewer the same movement shoves the cube away (-x) and pops it slightly.
        cube.addViewer(player);
        player.refreshPosition(new Pos(1.1, FLOOR_Y, 0.5));
        cube.tick(50);
        assertTrue(cube.getVelocity().x() < 0, "not pushed: " + cube.getVelocity());
        assertTrue(cube.getVelocity().y() > restingY + 1.0, "no ground pop: " + cube.getVelocity());
        assertEquals(0.0, cube.getVelocity().z(), 1e-9);
    }

    @Test
    void distantPlayerDoesNotPush() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var cube = spawnCube(instance, CUBE_SPAWN, Material.DIRT);
        tick(cube, 5);

        var player = new Player(new FakePlayerConnection(), new GameProfile(UUID.randomUUID(), "TestPlayer"));
        player.setInstance(instance, new Pos(2.5, FLOOR_Y, 0.5)).join();
        cube.addViewer(player);

        player.refreshPosition(new Pos(2.3, FLOOR_Y, 0.5)); // still > 1.3 blocks away
        cube.tick(0);
        assertEquals(0.0, cube.getVelocity().x(), 1e-9);
    }

    @Test
    void stationaryPlayerDoesNotPush() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var cube = spawnCube(instance, CUBE_SPAWN, Material.DIRT);
        tick(cube, 5);

        var player = new Player(new FakePlayerConnection(), new GameProfile(UUID.randomUUID(), "TestPlayer"));
        player.setInstance(instance, new Pos(1.1, FLOOR_Y, 0.5)).join();
        cube.addViewer(player);

        player.refreshPosition(player.getPosition()); // no movement this tick
        cube.tick(0);
        assertEquals(0.0, cube.getVelocity().x(), 1e-9);
    }
}
