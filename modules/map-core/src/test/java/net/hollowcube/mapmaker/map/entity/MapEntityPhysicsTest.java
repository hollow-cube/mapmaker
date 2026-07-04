package net.hollowcube.mapmaker.map.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static net.hollowcube.mapmaker.map.entity.PhysicsTestEnv.FLOOR_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapEntityPhysicsTest {

    /** Bounciness too small to visibly bounce, but enough to leave the stock Minestom fast path. */
    private static final double PIPELINE_ENABLING_BOUNCINESS = 0.0001;

    /** Entity with directly controllable physics knobs (they are attributes on living entities). */
    private static final class TestEntity extends MapEntity<EntityMeta> {
        double bounciness = 0.0;
        double frictionModifier = 1.0;
        double airDragModifier = 1.0;
        boolean omnidirectional = false;

        TestEntity() {
            super(EntityType.ZOMBIE);
        }

        @Override
        protected double bounciness() {
            return bounciness;
        }

        @Override
        protected double frictionModifier() {
            return frictionModifier;
        }

        @Override
        protected double airDragModifier() {
            return airDragModifier;
        }

        @Override
        protected boolean omnidirectionalAirDrag() {
            return omnidirectional;
        }
    }

    @BeforeAll
    static void init() {
        PhysicsTestEnv.init();
    }

    private static void tick(Entity entity, int count) {
        for (int i = 0; i < count; i++) entity.tick(50L * i);
    }

    /** Ticks until the entity gains upward velocity, or fails after {@code maxTicks}. */
    private static void tickUntilBounce(Entity entity, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            entity.tick(50L * i);
            if (entity.getVelocity().y() > 0) return;
        }
        throw new AssertionError("Entity never bounced within " + maxTicks + " ticks");
    }

    private static void setFloor(Instance instance, Block block) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                instance.setBlock(x, FLOOR_Y, z, block);
            }
        }
    }

    // computeModifiedFriction

    @Test
    void modifiedFrictionIdentityAtDefaultModifier() {
        assertEquals(0.6, MapEntity.computeModifiedFriction(0.6, 1.0), 1e-12);
        assertEquals(0.98, MapEntity.computeModifiedFriction(0.98, 1.0), 1e-12);
    }

    @Test
    void modifiedFrictionZeroModifierRemovesAllGrip() {
        assertEquals(1.0, MapEntity.computeModifiedFriction(0.6, 0.0), 1e-12);
    }

    @Test
    void modifiedFrictionClampsAtFullGrip() {
        assertEquals(0.0, MapEntity.computeModifiedFriction(0.6, 2048.0), 1e-12);
        assertEquals(0.0, MapEntity.computeModifiedFriction(0.05, 2.0), 1e-12);
    }

    // Pipeline

    @Test
    void defaultKnobsMatchStockMinestomExactly() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var custom = new TestEntity();
        var stock = new Entity(EntityType.ZOMBIE);
        var spawn = new Pos(0.5, FLOOR_Y + 5, 0.5);
        custom.setInstance(instance, spawn).join();
        stock.setInstance(instance, spawn).join();

        for (int i = 0; i < 60; i++) {
            custom.tick(50L * i);
            stock.tick(50L * i);
            assertEquals(stock.getPosition(), custom.getPosition(), "tick " + i);
            assertEquals(stock.getVelocity(), custom.getVelocity(), "tick " + i);
        }
    }

    @Test
    void bouncyEntityBouncesAndEventuallyRests() {
        var instance = PhysicsTestEnv.createFlatInstance();
        var entity = new TestEntity();
        entity.bounciness = 0.9;
        entity.setInstance(instance, new Pos(0.5, FLOOR_Y + 5, 0.5)).join();

        tickUntilBounce(entity, 100);

        // The first hop should recover a good part of the drop, but not all of it.
        double peak = entity.getPosition().y();
        for (int i = 0; i < 60; i++) {
            entity.tick(50L * i);
            peak = Math.max(peak, entity.getPosition().y());
        }
        assertTrue(peak > FLOOR_Y + 1.5, "first hop too low: " + peak);
        assertTrue(peak < FLOOR_Y + 5, "first hop gained energy: " + peak);

        // The one-gravity-tick cutoff must let it come to rest instead of micro-bouncing.
        tick(entity, 600);
        for (int i = 0; i < 50; i++) {
            entity.tick(50L * i);
            assertTrue(entity.getVelocity().y() <= 0, "still bouncing after settling");
            assertEquals(FLOOR_Y, entity.getPosition().y(), 1e-4);
        }
    }

    @Test
    void wallBounceReflectsHorizontalVelocity() {
        var instance = PhysicsTestEnv.createFlatInstance();
        for (int y = FLOOR_Y; y <= FLOOR_Y + 4; y++) {
            for (int z = -1; z <= 1; z++) {
                instance.setBlock(3, y, z, Block.STONE);
            }
        }

        var entity = new TestEntity();
        entity.bounciness = 0.5;
        entity.setNoGravity(true);
        entity.setInstance(instance, new Pos(0.5, FLOOR_Y + 2, 0.5)).join();
        entity.setVelocity(new Vec(10, 0, 0));

        for (int i = 0; i < 40 && entity.getVelocity().x() >= 0; i++) {
            entity.tick(50L * i);
        }

        assertTrue(entity.getVelocity().x() < 0, "wall did not reflect: " + entity.getVelocity());
        assertTrue(entity.getPosition().x() < 3, "clipped through the wall");
        // Half the impact speed, minus some air drag on the way in.
        assertTrue(entity.getVelocity().x() > -6 && entity.getVelocity().x() < -1,
            "unexpected bounce speed: " + entity.getVelocity());
    }

    @Test
    void slimeBlockBouncesEntityWithoutOwnBounciness() {
        var instance = PhysicsTestEnv.createFlatInstance();
        setFloor(instance, Block.SLIME_BLOCK);

        var entity = new TestEntity();
        entity.bounciness = PIPELINE_ENABLING_BOUNCINESS; // slime's 1.0 restitution dominates
        entity.setInstance(instance, new Pos(0.5, FLOOR_Y + 5, 0.5)).join();

        tickUntilBounce(entity, 100);

        // Slime restitution is 1.0, so the hop recovers what gravity/drag asymmetry allows
        // (roughly 3 of the 5 dropped blocks) - far more than the entity's own bounciness would.
        double peak = entity.getPosition().y();
        for (int i = 0; i < 60; i++) {
            entity.tick(50L * i);
            peak = Math.max(peak, entity.getPosition().y());
        }
        assertTrue(peak > FLOOR_Y + 2.5, "slime hop too low: " + peak);
    }

    @Test
    void honeyBlockSuppressesBounceEntirely() {
        var instance = PhysicsTestEnv.createFlatInstance();
        setFloor(instance, Block.HONEY_BLOCK);

        var entity = new TestEntity();
        entity.bounciness = 0.9;
        entity.setInstance(instance, new Pos(0.5, FLOOR_Y + 6, 0.5)).join();

        for (int i = 0; i < 200; i++) {
            entity.tick(50L * i);
            assertTrue(entity.getVelocity().y() <= 0, "bounced off honey at tick " + i);
        }
    }

    @Test
    void bedBouncesEntityWithoutOwnBounciness() {
        var instance = PhysicsTestEnv.createFlatInstance();
        setFloor(instance, Block.RED_BED);

        var entity = new TestEntity();
        entity.bounciness = PIPELINE_ENABLING_BOUNCINESS; // the bed's 0.75 restitution dominates
        entity.setInstance(instance, new Pos(0.5, FLOOR_Y + 5, 0.5)).join();

        tickUntilBounce(entity, 100);
    }

    @Test
    void frictionModifierControlsGroundSlide() {
        var instance = PhysicsTestEnv.createFlatInstance();

        var icy = new TestEntity();
        icy.frictionModifier = 0.05;
        icy.setInstance(instance, new Pos(0.5, FLOOR_Y, 0.5)).join();
        icy.setVelocity(new Vec(8, 0, 0));

        var sticky = new TestEntity();
        sticky.frictionModifier = 2.0;
        sticky.setInstance(instance, new Pos(0.5, FLOOR_Y, 4.5)).join();
        sticky.setVelocity(new Vec(8, 0, 0));

        tick(icy, 60);
        tick(sticky, 60);

        assertTrue(icy.getPosition().x() > sticky.getPosition().x() + 2,
            "icy=" + icy.getPosition().x() + " sticky=" + sticky.getPosition().x());
    }

    @Test
    void airDragModifierControlsAirSlowdown() {
        var instance = PhysicsTestEnv.createFlatInstance();

        var floaty = new TestEntity();
        floaty.airDragModifier = 0.01;
        floaty.setNoGravity(true);
        floaty.setInstance(instance, new Pos(0.5, FLOOR_Y + 5, 0.5)).join();
        floaty.setVelocity(new Vec(8, 0, 0));

        var draggy = new TestEntity();
        draggy.airDragModifier = 1.8;
        draggy.setNoGravity(true);
        draggy.setInstance(instance, new Pos(0.5, FLOOR_Y + 5, 4.5)).join();
        draggy.setVelocity(new Vec(8, 0, 0));

        tick(floaty, 40);
        tick(draggy, 40);

        assertTrue(floaty.getVelocity().x() > draggy.getVelocity().x() * 2,
            "floaty=" + floaty.getVelocity().x() + " draggy=" + draggy.getVelocity().x());
    }

    @Test
    void omnidirectionalAirDragAppliesHorizontalBaseVertically() {
        var instance = PhysicsTestEnv.createFlatInstance();

        var omni = new TestEntity();
        omni.bounciness = PIPELINE_ENABLING_BOUNCINESS;
        omni.omnidirectional = true;
        omni.setInstance(instance, new Pos(0.5, FLOOR_Y + 5, 0.5)).join();
        omni.setVelocity(new Vec(0, 40, 0));

        var regular = new TestEntity();
        regular.bounciness = PIPELINE_ENABLING_BOUNCINESS;
        regular.setInstance(instance, new Pos(0.5, FLOOR_Y + 5, 4.5)).join();
        regular.setVelocity(new Vec(0, 40, 0));

        tick(omni, 10);
        tick(regular, 10);

        // Vertical drag base 0.91 vs 0.98: the omnidirectional mover sheds upward speed much
        // faster while both are still rising (expected ~0.29 vs ~0.92 blocks/tick here).
        assertTrue(omni.getVelocity().y() > 0 && regular.getVelocity().y() > 0,
            "both should still be rising: omni=" + omni.getVelocity().y() + " regular=" + regular.getVelocity().y());
        assertTrue(omni.getVelocity().y() < regular.getVelocity().y() / 2,
            "omni=" + omni.getVelocity().y() + " regular=" + regular.getVelocity().y());
    }
}
