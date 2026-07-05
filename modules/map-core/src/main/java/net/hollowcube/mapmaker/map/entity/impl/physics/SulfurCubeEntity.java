package net.hollowcube.mapmaker.map.entity.impl.physics;

import net.hollowcube.common.math.MathUtil;
import net.hollowcube.common.math.Vec2;
import net.hollowcube.common.util.FluidUtil;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.metadata.cube.SulfurCubeArchetype;
import net.minestom.server.entity.metadata.cube.SulfurCubeMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SulfurCubeEntity extends AbstractMobEntity<SulfurCubeMeta> {

    public static final MapEntityInfo<SulfurCubeEntity> INFO = MapEntityInfo.<SulfurCubeEntity>builder(AbstractLivingEntity.INFO)
        .build();

    public static @Nullable SulfurCubeArchetype archetypeFor(Material material) {
        for (var archetype : MinecraftServer.process().sulfurCubeArchetype().values()) {
            if (archetype.items().contains(material)) return archetype;
        }
        return null;
    }

    private static final int ADULT_SIZE = 2;
    private static final double BOUNDING_BOX_SIZE = 0.49 * ADULT_SIZE; // notably different from slime

    // vanilla numbers
    private static final float HORIZONTAL_HIT_ANGLE_SCALE = 1.6f;
    private static final float VERTICAL_HIT_ANGLE_SCALE = 0.5f;
    private static final float VERTICAL_POSITION_ANGLE_SCALE = 0.8f;
    private static final double PUSH_DISTANCE_THRESHOLD = 1.3;
    private static final double MAX_PLAYER_PUSH_SPEED = 0.5;
    private static final float PLAYER_PUSH_SPEED_SCALE = 0.3f;
    private static final float VEHICLE_PUSH_SPEED_SCALE = 0.16f;
    private static final float VERTICAL_PUSH_SCALE = 0.3f;

    private record Config(SulfurCubeArchetype.KnockbackModifiers knockback,
                                    SulfurCubeArchetype.SoundSettings sounds,
                                    boolean buoyant) {
        private static final Config DEFAULT = new Config(
            SulfurCubeArchetype.KnockbackModifiers.DEFAULT,
            SulfurCubeArchetype.SoundSettings.DEFAULT,
            false);
    }

    private final Set<Attribute> appliedAttributes = new HashSet<>();
    private Config config = Config.DEFAULT;
    private int pushSoundCooldownTicks = 0;

    public SulfurCubeEntity(UUID uuid) {
        super(EntityType.SULFUR_CUBE, uuid);
        getEntityMeta().setSize(ADULT_SIZE);
        setBoundingBox(BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE);
    }

    // Body block / archetype

    public void setBodyBlock(ItemStack blockItem) {
        setEquipment(EquipmentSlot.BODY, blockItem);
    }

    public boolean hasBodyBlock() {
        return !getEquipment(EquipmentSlot.BODY).isAir();
    }

    public void setArchetype(@Nullable SulfurCubeArchetype archetype) {
        // reset
        for (var attribute : appliedAttributes) setAttribute(attribute, attribute.defaultValue());
        appliedAttributes.clear();
        config = Config.DEFAULT;
        if (archetype == null) return;

        config = new Config(archetype.knockbackModifiers(), archetype.soundSettings(), archetype.buoyant());

        var modifiers = new HashMap<Attribute, List<AttributeModifier>>();
        for (var entry : archetype.attributeModifiers()) {
            modifiers.computeIfAbsent(entry.attribute(), a -> new ArrayList<>()).add(entry.modifier());
        }
        modifiers.forEach((attribute, list) -> {
            setAttribute(attribute, attribute.defaultValue(), list);
            appliedAttributes.add(attribute);
        });
    }

    // Hit knockback

    public void applyHitKnockback(Entity attacker) {
        var attackerPosition = attacker.getPosition();
        applyHitKnockback(attackerPosition, attackerPosition.add(0, attacker.getEyeHeight(), 0),
            attackerPosition.direction(), 1.0f);
    }

    // port of vanilla knockback logic, probably can be generalized a bunch in the future.
    public void applyHitKnockback(Point attackerFeet, Point attackerEye, Vec lookDirection, float damage) {
        var look = lookDirection.normalize();
        var center = new Vec(position.x(), position.y() + boundingBox.height() / 2, position.z());

        // Base knockback direction, attacker -> cube; negated on application below, exactly
        // like vanilla dealDefaultKnockback + knockback.
        var direction = new Vec2(attackerFeet.x() - position.x(), attackerFeet.z() - position.z());

        double originalHorizontal = config.knockback().horizontalPower();
        double originalVertical = config.knockback().verticalPower();

        // 1. Steer the horizontal direction toward where the attacker is aiming.
        var toTarget = center.sub(attackerEye).normalize();
        double aimAngle = Math.atan2(
            look.x() * toTarget.z() - look.z() * toTarget.x(),
            look.x() * toTarget.x() + look.z() * toTarget.z());
        direction = direction.rotate(aimAngle * HORIZONTAL_HIT_ANGLE_SCALE);

        // 2. Vertical aim transfers power between the axes: aiming at the bottom of the cube
        //    pops it up, aiming at the top slams it flat.
        double halfHeight = boundingBox.height() / 2;
        double toTopY = center.add(0, halfHeight, 0).sub(attackerEye).normalize().y();
        double toBottomY = center.sub(0, halfHeight, 0).sub(attackerEye).normalize().y();
        double aimFactor = MathUtil.clampedMap(look.y(), toTopY, toBottomY, -1.0, 1.0);
        double transferRatio = Math.copySign(Math.abs(aimFactor * VERTICAL_HIT_ANGLE_SCALE), aimFactor);
        var power = new Vec2(originalHorizontal * (1.0 - transferRatio), originalVertical * (1.0 + transferRatio));

        // 3. Rotate the power by the relative height of attacker and cube, clamped back to
        //    the original magnitudes.
        double feetDx = position.x() - attackerFeet.x();
        double feetDy = position.y() - attackerFeet.y();
        double feetDz = position.z() - attackerFeet.z();
        double heightAngle = Math.atan2(-feetDy, Math.sqrt(feetDx * feetDx + feetDz * feetDz));
        power = power.rotate(-heightAngle * VERTICAL_POSITION_ANGLE_SCALE);
        double horizontalRatio = originalHorizontal > 0 ? Math.abs(power.x()) / originalHorizontal : 0;
        double verticalRatio = originalVertical > 0 ? Math.abs(power.y()) / originalVertical : 0;
        double maxRatio = Math.max(horizontalRatio, verticalRatio);
        if (maxRatio > 1) power = new Vec2(power.x() / maxRatio, power.y() / maxRatio);

        // Finalize: sqrt(damage) power multiplier, knockback resistance (negative for most
        // archetypes, amplifying the hit), then clamp. Powers are in blocks/tick until the
        // final conversion.
        double powerMultiplier = Math.sqrt(damage) * (1.0 - getAttribute(Attribute.KNOCKBACK_RESISTANCE));
        double horizontalPower = Math.clamp(power.x() * powerMultiplier * 0.4, -128, 128);
        double verticalPower = Math.clamp(power.y() * powerMultiplier, -128, 128);

        double length = Math.sqrt(direction.x() * direction.x() + direction.y() * direction.y());
        double directionX = length < Vec.EPSILON ? 0 : direction.x() / length;
        double directionZ = length < Vec.EPSILON ? 0 : direction.y() / length;

        velocity = velocity.add(
            -directionX * horizontalPower * ServerFlag.SERVER_TICKS_PER_SECOND,
            verticalPower * 1.2 * ServerFlag.SERVER_TICKS_PER_SECOND,
            -directionZ * horizontalPower * ServerFlag.SERVER_TICKS_PER_SECOND);
        playCubeSound(config.sounds().hitSound());
        if (hasVelocity()) sendPacketToViewers(getVelocityPacket());
    }

    // Per-tick behavior

    @Override
    public void update(long time) {
        super.update(time);
        if (isRemoved()) return;

        // If we have a body the yaw should always remain snapped to a multiple of 90 degrees
        if (hasBodyBlock()) {
            float yaw = position.yaw();
            float snapped = Math.round(yaw / 90.0f) * 90.0f;
            if (yaw != snapped) setView(snapped, position.pitch());
        }

        pushTick();
    }

    private void pushTick() {
        if (pushSoundCooldownTicks > 0) pushSoundCooldownTicks--;
        if (!hasBodyBlock()) return;
        var instance = getInstance();
        if (instance == null) return;

        boolean pushed = false;
        for (var entity : instance.getNearbyEntities(position, 3.0)) {
            if (!(entity instanceof Player player)) continue;
            // Mapmaker may run per-player instances: a player who cannot see this cube must
            // not interact with it. (Vanilla pushes on any contact.)
            if (!getViewers().contains(player)) continue;

            Entity pusher = player;
            while (pusher.getVehicle() != null) pusher = pusher.getVehicle();
            boolean riding = pusher != player;

            var pusherPosition = pusher.getPosition();
            double dx = position.x() - pusherPosition.x();
            double dz = position.z() - pusherPosition.z();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDistance >= PUSH_DISTANCE_THRESHOLD || horizontalDistance < Vec.EPSILON) continue;

            double cubeTop = position.y() + boundingBox.height();
            double pusherTop = pusherPosition.y() + pusher.getBoundingBox().height();
            if (pusherPosition.y() > cubeTop || pusherTop <= position.y()) continue;

            double knockback = Math.max(0.0, 1.0 - getAttribute(Attribute.KNOCKBACK_RESISTANCE));
            // The player's actual movement this tick, in blocks/tick.
            double playerSpeed = player.getPosition().sub(player.getPreviousPosition()).asVec().length();
            double speed = Math.clamp(
                playerSpeed * 2.0 * (riding ? VEHICLE_PUSH_SPEED_SCALE : PLAYER_PUSH_SPEED_SCALE),
                0.0, MAX_PLAYER_PUSH_SPEED);

            var push = new Vec(
                dx / horizontalDistance * knockback,
                onGround ? knockback * VERTICAL_PUSH_SCALE : 0.0,
                dz / horizontalDistance * knockback).mul(speed);

            float threshold = config.sounds().pushSoundImpulseThreshold();
            if (push.lengthSquared() > threshold * threshold && pushSoundCooldownTicks <= 0) {
                pushSoundCooldownTicks = (int) (config.sounds().pushSoundCooldown() * ServerFlag.SERVER_TICKS_PER_SECOND);
                playCubeSound(config.sounds().pushSound());
            }

            velocity = velocity.add(push.mul(ServerFlag.SERVER_TICKS_PER_SECOND));
            pushed = true;
        }
        if (pushed && hasVelocity()) sendPacketToViewers(getVelocityPacket());
    }

    private void playCubeSound(SoundEvent sound) {
        var random = ThreadLocalRandom.current();
        float volume = 0.4f * getEntityMeta().getSize();
        float pitch = ((random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f) * 0.8f;
        playSound(sound, volume, pitch);
    }

    // Physics hooks

    @Override
    protected boolean omnidirectionalAirDrag() {
        return hasBodyBlock();
    }

    @Override
    protected Vec applyPhysicsGravityAndDrag(Block.Getter blockGetter, PhysicsResult result,
                                             Pos newPosition, Vec velocityPerTick) {
        var fluids = FluidUtil.scan(blockGetter, boundingBox, newPosition);
        if (fluids.inWater() || fluids.inLava()) {
            return applyFluidGravityAndDrag(fluids, velocityPerTick);
        }
        return super.applyPhysicsGravityAndDrag(blockGetter, result, newPosition, velocityPerTick);
    }

    private Vec applyFluidGravityAndDrag(FluidUtil.Result fluids, Vec velocityPerTick) {
        boolean falling = velocityPerTick.y() <= 0;
        double gravity = hasNoGravity() ? 0.0 : getAerodynamics().gravity();

        Vec movement = velocityPerTick.mul(0.8);
        if (gravity != 0) {
            // getFluidFallingAdjustedMovement: gravity is 16x weaker in fluid, with a
            // -0.003/tick terminal trickle once the fall settles.
            if (falling && Math.abs(movement.y() - 0.005) >= 0.003 && Math.abs(movement.y() - gravity / 16.0) < 0.003) {
                movement = movement.withY(-0.003);
            } else {
                movement = movement.withY(movement.y() - gravity / 16.0);
            }
        }

        if (config.buoyant()) {
            double fluidHeight = fluids.inWater() ? fluids.waterHeight() : fluids.lavaHeight();
            double immersion = fluidHeight - boundingBox.height() * 0.2 + 0.2 * Math.sin(getAliveTicks() * 0.4);
            if (immersion > 0) movement = movement.withY(movement.y() + Math.min(1.0, immersion) * 0.04);
        }
        return movement;
    }

    // Serialization

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Data is stored in body item + attributes which are handled by livingentity.
    }
}
