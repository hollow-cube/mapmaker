package net.hollowcube.mapmaker.map.entity.impl.projectile;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.ServerFlag;
import net.minestom.server.collision.Aerodynamics;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.WeightedList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class WindChargeEntity extends AbstractProjectileEntity {
    private static final float EXPLOSION_RADIUS = 1.2f;
    private static final float PROJECTILE_SHOOT_POWER = 1.5f;

    public static @NotNull WindChargeEntity shootFromPlayerDirection(@NotNull Player shooter, boolean shooterOnly) {
        shooter.playSound(Sound.sound(SoundEvent.ENTITY_WIND_CHARGE_THROW, Sound.Source.NEUTRAL,
            0.5f, 0.4f / (ThreadLocalRandom.current().nextFloat() * 0.4f + 0.8f)));

        var entity = new WindChargeEntity(shooter);
        if (shooterOnly) entity.setAutoViewable(false);
        entity.shoot(shooter.getPosition().add(0, shooter.getEyeHeight(), 0), PROJECTILE_SHOOT_POWER, 0f);
        if (shooterOnly) entity.addViewer(shooter);
        return entity;
    }

    private WindChargeEntity(@NotNull Player shooter) {
        super(EntityType.WIND_CHARGE, shooter);
    }

    @Override
    protected void movementTick() {
        super.movementTick();

        var instance = getInstance();
        if (instance != null && getPosition().y() < instance.getCachedDimensionType().minY() - 32)
            remove();
        if (shooter.getInstance() != instance)
            remove();
    }

    @Override
    protected boolean callEntityCollision() {
        return false; // do not remove on collide
    }

    @Override
    protected void handleBlockCollision(Block hitBlock, Point hitPos, Pos posBefore) {
        super.handlePossibleDripleafCollision(shooter, hitBlock, hitPos);

        sendExplosion(getViewers(), hitPos, EXPLOSION_RADIUS, 1, SoundEvent.ENTITY_WIND_CHARGE_WIND_BURST,
            Particle.GUST_EMITTER_SMALL, Particle.GUST_EMITTER_LARGE, true, true);

        remove();
    }

    public static void sendExplosion(
        Collection<? extends Player> players,
        Point hitPos,
        float radius,
        float knockbackMultiplier,
        SoundEvent sound,
        Particle smallParticle,
        Particle bigParticle,
        boolean forceSmall,
        boolean useLegacyWindChargeLogic
    ) {

        float diameter = radius * 2.0f;
        // these values are the bounding box of the explosion aka only move those people ever.
        int $$1 = (int) Math.floor(hitPos.x() - diameter - 1.0);
        int $$2 = (int) Math.floor(hitPos.x() + diameter + 1.0);
        int $$3 = (int) Math.floor(hitPos.y() - diameter - 1.0);
        int $$4 = (int) Math.floor(hitPos.y() + diameter + 1.0);
        int $$5 = (int) Math.floor(hitPos.z() - diameter - 1.0);
        int $$6 = (int) Math.floor(hitPos.z() + diameter + 1.0);

        for (var player : players) {
            double distance = Math.sqrt(player.getDistanceSquared(hitPos));
            double $$10 = player.getPosition().x() - hitPos.x();
            double $$11 = (player.getPosition().y() + player.getEyeHeight()) - hitPos.y();
            double $$12 = player.getPosition().z() - hitPos.z();
            double eyeDistance = Math.sqrt($$10 * $$10 + $$11 * $$11 + $$12 * $$12);

            Vec motion = Vec.ZERO;
            if (!(!(distance / diameter <= 1.0) || eyeDistance == 0.0)) {
                $$10 /= eyeDistance;
                $$11 /= eyeDistance;
                $$12 /= eyeDistance;

                float kbMult = player.isFlying() ? 0 : knockbackMultiplier;
                float $$16 = kbMult != 0.0f ? getSeenPercent(hitPos, player) : 0.0f;
                double $$17 = (1.0 - distance) * (double) $$16 * (double) kbMult;

                double $$20 = useLegacyWindChargeLogic ? 1 : $$17 * (1.0 - player.getAttributeValue(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE));
                motion = new Vec($$10 * $$20, $$11 * $$20, $$12 * $$20);
            }

            boolean isSmall = radius < 2f || forceSmall;
            if (player instanceof MapPlayer mp)
                mp.trackImpulsePosition(mp.getPosition(), true);
            player.sendPacket(new ExplosionPacket(
                hitPos,
                radius,
                0,
                motion,
                isSmall ? smallParticle : bigParticle,
                sound,
                WeightedList.of()
            ));
        }
    }

    public static float getSeenPercent(Point center, Player player) {
        return 1f;
//        var boundingBox = player.getBoundingBox();
//        double $$3 = 1.0 / ((boundingBox.maxX() - boundingBox.minX()) * 2.0 + 1.0);
//        double $$4 = 1.0 / ((boundingBox.maxY() - boundingBox.minY()) * 2.0 + 1.0);
//        double $$5 = 1.0 / ((boundingBox.maxZ() - boundingBox.minZ()) * 2.0 + 1.0);
//        double $$6 = (1.0 - Math.floor(1.0 / $$3) * $$3) / 2.0;
//        double $$7 = (1.0 - Math.floor(1.0 / $$5) * $$5) / 2.0;
//        if ($$3 < 0.0 || $$4 < 0.0 || $$5 < 0.0) {
//            return 0.0f;
//        }
//        int $$8 = 0;
//        int $$9 = 0;
//        for (double $$10 = 0.0; $$10 <= 1.0; $$10 += $$3) {
//            for (double $$11 = 0.0; $$11 <= 1.0; $$11 += $$4) {
//                for (double $$12 = 0.0; $$12 <= 1.0; $$12 += $$5) {
//                    double $$13 = Mth.lerp($$10, boundingBox.minX, boundingBox.maxX);
//                    double $$14 = Mth.lerp($$11, boundingBox.minY, boundingBox.maxY);
//                    double $$15 = Mth.lerp($$12, boundingBox.minZ, boundingBox.maxZ);
//                    Vec3 $$16 = new Vec3($$13 + $$6, $$14, $$15 + $$7);
//                    if (player.level().clip(new ClipContext($$16, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS) {
//                        ++$$8;
//                    }
//                    ++$$9;
//                }
//            }
//        }
//        return (float)$$8 / (float)$$9;
    }

    @Override
    public void shoot(@NotNull Point from, @NotNull Point to, double power, double spread) {
        var instance = shooter.getInstance();
        if (instance == null) return;

        float yaw = -shooter.getPosition().yaw();
        float originalPitch = -shooter.getPosition().pitch();

        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();

        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= length;
        dy /= length;
        dz /= length;
        Random random = ThreadLocalRandom.current();
        spread *= 0.007499999832361937D;
        dx += random.nextGaussian() * spread;
        dy += random.nextGaussian() * spread;
        dz += random.nextGaussian() * spread;

        final EntityShootEvent shootEvent = new EntityShootEvent(this.shooter, this, from, power, spread);
        EventDispatcher.call(shootEvent);
        if (shootEvent.isCancelled()) {
            remove();
            return;
        }

        final double mul = ServerFlag.SERVER_TICKS_PER_SECOND * power;
        Vec v = new Vec(dx * mul, dy * mul, dz * mul); // zero upwards force for wind charge
        this.setInstance(instance, new Pos(from.x(), from.y() - this.boundingBox.height() / 2, from.z(), yaw, originalPitch)).whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                synchronizePosition(); // initial synchronization, required to be 100% precise
                setVelocity(v);
            }
        });
    }

    protected @NotNull Vec updateVelocity(@NotNull Pos entityPosition, @NotNull Vec currentVelocity, @NotNull Block.@NotNull Getter blockGetter, @NotNull Aerodynamics aerodynamics, boolean positionChanged, boolean entityFlying, boolean entityOnGround, boolean entityNoGravity) {
        double x = currentVelocity.x();
        double y = currentVelocity.y();
        double z = currentVelocity.z();
        return new Vec(Math.abs(x) < 1.0E-6 ? 0.0 : x, Math.abs(y) < 1.0E-6 ? 0.0 : y, Math.abs(z) < 1.0E-6 ? 0.0 : z);
    }
}
