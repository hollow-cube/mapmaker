package net.hollowcube.mapmaker.map.entity.marker.builtin;

import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerHandler;
import net.hollowcube.mql.jit.MqlCompiler;
import net.hollowcube.mql.jit.ValueScript;
import net.kyori.adventure.nbt.*;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadLocalRandom;

public class ParticleEmitterMarkerHandler extends MarkerHandler {
    public static final String ID = "mapmaker:particle_emitter";

    private static final MqlCompiler<ValueScript> COMPILER;

    static {
        try {
            COMPILER = new MqlCompiler<>(MethodHandles.privateLookupIn(ValueScript.class, MethodHandles.lookup()), ValueScript.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValid = false;
    private int lifetime; // Loop duration, in ticks. 0 for infinite
    private double rate; // Particles per tick
    private Particle particle;
    private ValueScript positionX;
    private ValueScript positionY;
    private ValueScript positionZ;
    // Speed, count and offsetXYZ are mutually exclusive with velocityXYZ
    private ValueScript speed;
    private ValueScript count;
    private ValueScript offsetX;
    private ValueScript offsetY;
    private ValueScript offsetZ;
    // See above comment
    private ValueScript velocityX;
    private ValueScript velocityY;
    private ValueScript velocityZ;

    private final ValueScript.Variables variables = new ValueScript.Variables();
    private double toSpawn = 0;
    private int age = -1; // Current loop age

    public ParticleEmitterMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);
        onDataChange(null);
    }

    @Override
    protected void onDataChange(@Nullable Player player) {
        try {
            loadParticleData(entity.getMarkerData());
            this.age = -1; // Reset
            this.toSpawn = 0;
            this.isValid = true;
        } catch (IllegalArgumentException e) {
            if (player != null)
                player.sendMessage("Invalid particle data: " + e.getMessage());
            this.isValid = false;
        }
    }

    @Override
    protected void onTick() {
        if (!isValid) return;

        // Reset state on start and when reaching lifetime
        if (age == -1 || (lifetime > 0 && age >= lifetime)) {
            age = 0;
            variables.lifetime = lifetime;
            variables.random1 = ThreadLocalRandom.current().nextDouble();
            variables.random2 = ThreadLocalRandom.current().nextDouble();
            variables.random3 = ThreadLocalRandom.current().nextDouble();
            variables.random4 = ThreadLocalRandom.current().nextDouble();
        }

        variables.age = age++;
        toSpawn += rate;

        while (toSpawn >= 1) {
            toSpawn--;
            Point position = entity.getPosition();
            if (positionX != null) {
                position = position.add(
                        positionX.eval(variables),
                        positionY.eval(variables),
                        positionZ.eval(variables)
                );
            }

            float computedSpeed;
            int computedCount;
            Vec computedOffset;
            if (speed != null || count != null || offsetX != null) {
                computedSpeed = (float) (speed != null ? speed.eval(variables) : 0);
                double evaledCount = count != null ? count.eval(variables) : 1;
                computedCount = evaledCount < 1 ? 1 : (int) evaledCount;
                computedOffset = new Vec(
                        offsetX != null ? offsetX.eval(variables) : 0,
                        offsetY != null ? offsetY.eval(variables) : 0,
                        offsetZ != null ? offsetZ.eval(variables) : 0
                );
            } else if (velocityX != null) {
                var computedVelocity = new Vec(
                        velocityX.eval(variables),
                        velocityY.eval(variables),
                        velocityZ.eval(variables)
                );
                computedSpeed = (float) computedVelocity.length();
                computedCount = 0;
                computedOffset = computedVelocity.normalize();
            } else {
                computedSpeed = 0;
                computedCount = 1;
                computedOffset = Vec.ZERO;
            }

            entity.sendPacketToViewers(new ParticlePacket(particle, false, position, computedOffset, computedSpeed, computedCount));
        }
    }

    private void loadParticleData(@NotNull CompoundBinaryTag data) throws IllegalArgumentException {
        var keys = data.keySet();

        // Particle
        var particleName = data.getString("particle");
        Check.argCondition(particleName.isEmpty(), "Missing particle name");
        particle = Particle.fromNamespaceId(particleName);
        Check.argCondition(particle == null, "Unknown particle: " + particleName);
        readTypedParticleData(data);

        // Rate
        if (keys.contains("rate")) {
            if (!(data.get("rate") instanceof NumberBinaryTag rateTag))
                throw new IllegalArgumentException("rate: expected number, got " + data.get("rate").getClass().getSimpleName());
            Check.argCondition(rateTag.doubleValue() <= 0, "rate: must be positive");
            this.rate = rateTag.doubleValue();
        } else this.rate = 1;

        // Lifetime
        if (keys.contains("lifetime")) {
            if (!(data.get("lifetime") instanceof NumberBinaryTag lifetimeTag))
                throw new IllegalArgumentException("lifetime: expected int, got " + data.get("lifetime").getClass().getSimpleName());
            Check.argCondition(lifetimeTag.intValue() < 0, "lifetime: must be non-negative");
            this.lifetime = lifetimeTag.intValue();
        } else this.lifetime = 0;

        // Speed
        speed = loadValueScript("speed", data.get("speed"));

        // Position
        if (keys.contains("position")) {
            var positionTag = assertVecTag("position", data.get("position"));
            positionX = loadValueScript("position.x", positionTag.get(0));
            positionY = loadValueScript("position.y", positionTag.get(1));
            positionZ = loadValueScript("position.z", positionTag.get(2));
        } else positionX = positionY = positionZ = null;

        // Count
        count = loadValueScript("count", data.get("count"));

        // Offset
        if (keys.contains("offset")) {
            var offsetTag = assertVecTag("offset", data.get("offset"));
            offsetX = loadValueScript("offset.x", offsetTag.get(0));
            offsetY = loadValueScript("offset.y", offsetTag.get(1));
            offsetZ = loadValueScript("offset.z", offsetTag.get(2));
        } else offsetX = offsetY = offsetZ = null;

        // Velocity
        if (keys.contains("velocity")) {
            var velocityTag = assertVecTag("velocity", data.get("velocity"));
            velocityX = loadValueScript("velocity.x", velocityTag.get(0));
            velocityY = loadValueScript("velocity.y", velocityTag.get(1));
            velocityZ = loadValueScript("velocity.z", velocityTag.get(2));
        } else velocityX = velocityY = velocityZ = null;

        // Final validity checks
        boolean hasOffset = speed != null || count != null || offsetX != null || offsetY != null || offsetZ != null;
        boolean hasVelocity = velocityX != null || velocityY != null || velocityZ != null;
        Check.argCondition(hasOffset && hasVelocity, "Cannot have both count/offset and velocity");
    }

    private ListBinaryTag assertVecTag(@NotNull String name, @Nullable BinaryTag tag) {
        if (tag == null) throw new IllegalArgumentException(name + ": missing");
        if (!(tag instanceof ListBinaryTag vecTag))
            throw new IllegalArgumentException(name + ": expected list, got " + tag.getClass().getSimpleName());
        if (vecTag.size() != 3)
            throw new IllegalArgumentException(name + ": expected 3 elements, got " + vecTag.size());
        if (!isNumberOrScript(vecTag.elementType()))
            throw new IllegalArgumentException(name + ": expected number or script elements, got " + vecTag.elementType());
        return vecTag;
    }

    private boolean isNumberOrScript(@NotNull BinaryTagType<?> type) {
        return type == BinaryTagTypes.STRING || type == BinaryTagTypes.BYTE || type == BinaryTagTypes.SHORT || type == BinaryTagTypes.INT || type == BinaryTagTypes.LONG || type == BinaryTagTypes.FLOAT || type == BinaryTagTypes.DOUBLE;
    }

    private @Nullable ValueScript loadValueScript(@NotNull String name, @Nullable BinaryTag tag) {
        if (tag == null) return null;
        if (tag instanceof StringBinaryTag scriptTag) {
            try {
                return COMPILER.compile(scriptTag.value()).newInstance(); //todo dont use newinstance
            } catch (Throwable e) {
                throw new IllegalArgumentException(name + ": failed to compile script: " + e.getMessage());
            }
        } else if (tag instanceof NumberBinaryTag numberTag) {
            return _ -> numberTag.doubleValue();
        } else {
            throw new IllegalArgumentException(name + ": expected number or script, got " + tag.getClass().getSimpleName());
        }
    }

    private void readTypedParticleData(@NotNull CompoundBinaryTag data) throws IllegalArgumentException {
        switch (particle) {
            case Particle.Block blockParticle when data.get("block") instanceof StringBinaryTag blockName -> {
                try {
                    particle = blockParticle.withBlock(ArgumentBlockState.staticParse(blockName.value()));
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalArgumentException("Invalid block state: " + e.getMessage());
                }
            }
            case Particle.BlockMarker blockMarkerParticle when data.get("block") instanceof StringBinaryTag blockName -> {
                try {
                    particle = blockMarkerParticle.withBlock(ArgumentBlockState.staticParse(blockName.value()));
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalArgumentException("Invalid block state: " + e.getMessage());
                }
            }
            case Particle.Dust dustParticle -> {
                //todo need to support expressions in here.
            }
            case Particle.DustColorTransition dustColorTransitionParticle -> {
                //todo
            }
            case Particle.DustPillar dustPillarParticle when data.get("block") instanceof StringBinaryTag blockName -> {
                try {
                    particle = dustPillarParticle.withBlock(ArgumentBlockState.staticParse(blockName.value()));
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalArgumentException("Invalid block state: " + e.getMessage());
                }
            }
            // EntityEffect
            case Particle.FallingDust fallingDustParticle when data.get("block") instanceof StringBinaryTag blockName -> {
                try {
                    particle = fallingDustParticle.withBlock(ArgumentBlockState.staticParse(blockName.value()));
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalArgumentException("Invalid block state: " + e.getMessage());
                }
            }
            case Particle.Item itemParticle when data.get("item") instanceof StringBinaryTag itemName -> {
                var material = Material.fromNamespaceId(itemName.value());
                Check.argCondition(material == null, "Unknown item: " + itemName);
                particle = itemParticle.withItem(ItemStack.of(material));
                //todo should support custom model data and maybe other components.
            }
            // SculkCharge, Shriek, Vibration
            default -> {
            } // No data or unsupported
        }
    }
}
