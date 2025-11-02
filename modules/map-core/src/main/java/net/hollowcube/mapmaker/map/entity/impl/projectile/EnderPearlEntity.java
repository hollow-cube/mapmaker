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
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class EnderPearlEntity extends AbstractProjectileEntity {
    private static final float PROJECTILE_SHOOT_POWER = 1.5f;

    public static @NotNull EnderPearlEntity shootFromPlayerDirection(@NotNull Player shooter, boolean shooterOnly) {
        shooter.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_PEARL_THROW, Sound.Source.NEUTRAL,
                0.5f, 0.4f / (ThreadLocalRandom.current().nextFloat() * 0.4f + 0.8f)));
        var entity = new EnderPearlEntity(shooter);
        if (shooterOnly) entity.setAutoViewable(false);
        entity.shoot(shooter.getPosition().add(0, shooter.getEyeHeight() - 0.1, 0), PROJECTILE_SHOOT_POWER, 0f);
        if (shooterOnly) entity.addViewer(shooter);
        return entity;
    }

    private EnderPearlEntity(@NotNull Player shooter) {
        super(EntityType.ENDER_PEARL, shooter);
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

        remove();

        shooter.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 1, 1), posBefore);
        shooter.teleport(posBefore.withView(0, 0), Vec.ZERO,
                null, RelativeFlags.VIEW | RelativeFlags.DELTA_COORD);
        if (shooter instanceof MapPlayer mp) {
            mp.resetFallDistance();
            mp.resetImpulsePosition();
        }
    }

    @Override
    public void shoot(@NotNull Point from, @NotNull Point to, double power, double spread) {
        // TODO: clean this up its yoinked from atlas projectiles and kinda cursed and maybe just wrong?
        var instance = shooter.getInstance();
        if (instance == null) return;

        float yaw = -shooter.getPosition().yaw();
        float originalPitch = -shooter.getPosition().pitch();

        double pitchDiff = originalPitch - 45f;
        if (pitchDiff == 0) pitchDiff = 0.0001;
        double pitchAdjust = pitchDiff * 0.002145329238474369D;

        double dx = to.x() - from.x();
        double dy = to.y() - from.y() + pitchAdjust;
        double dz = to.z() - from.z();

        if (!hasNoGravity()) {
            final double xzLength = Math.sqrt(dx * dx + dz * dz);
            dy += xzLength * 0.20000000298023224D;
        }

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
        Vec v = new Vec(dx * mul, dy * mul * 0.9, dz * mul);

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
        double y = currentVelocity.y() - 0.03;
        double z = currentVelocity.z();
        return new Vec(Math.abs(x) < 1.0E-6 ? 0.0 : x, Math.abs(y) < 1.0E-6 ? 0.0 : y, Math.abs(z) < 1.0E-6 ? 0.0 : z);
    }
}
