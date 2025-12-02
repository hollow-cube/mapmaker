package net.hollowcube.mapmaker.map.entity.marker.builtin;

import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.molang.MolangExpr;
import net.hollowcube.molang.eval.MolangEvaluator;
import net.hollowcube.molang.eval.MolangValue;
import net.kyori.adventure.nbt.*;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.color.Color;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.player.ClientSettings;
import net.minestom.server.particle.Particle;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class ParticleEmitterMarkerHandler extends ObjectEntityHandler {
    private static final double MAX_PARTICLES_PER_SECOND = 100;
    public static final String ID = "mapmaker:particle_emitter";
    private static final Logger logger = LoggerFactory.getLogger(ParticleEmitterMarkerHandler.class);

    private boolean isValid = false;
    private int lifetime; // Loop duration, in ticks. 0 for infinite
    private double rate; // Particles per tick
    private Supplier<Particle> particle;
    private MolangExpr positionX;
    private MolangExpr positionY;
    private MolangExpr positionZ;
    // Speed, count and offsetXYZ are mutually exclusive with velocityXYZ
    private MolangExpr speed;
    private MolangExpr count;
    private MolangExpr offsetX;
    private MolangExpr offsetY;
    private MolangExpr offsetZ;
    // See above comment
    private MolangExpr velocityX;
    private MolangExpr velocityY;
    private MolangExpr velocityZ;
    // If not provided or 1, particles will be spawned. Otherwise they will not.
    private MolangExpr active;

    private final Variables variables = new Variables();
    private final MolangEvaluator molangEval = new MolangEvaluator(Map.of(
        "variable", variables,
        "v", variables,
        "query", Queries.INSTANCE,
        "q", Queries.INSTANCE
    ));
    private double toSpawn = 0;
    private int age = -1; // Current loop age

    public ParticleEmitterMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);
        onDataChange(null);
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        try {
            loadParticleData(entity.getData());
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
    public void onTick() {
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

            if (active != null && !molangEval.evalBool(active))
                continue;

            variables.particleRandom1 = ThreadLocalRandom.current().nextDouble();
            variables.particleRandom2 = ThreadLocalRandom.current().nextDouble();
            variables.particleRandom3 = ThreadLocalRandom.current().nextDouble();
            variables.particleRandom4 = ThreadLocalRandom.current().nextDouble();

            Point position = entity.getPosition();
            if (positionX != null) {
                position = position.add(
                    molangEval.eval(positionX),
                    molangEval.eval(positionY),
                    molangEval.eval(positionZ)
                );
            }

            float computedSpeed;
            int computedCount;
            Vec computedOffset;
            if (speed != null || count != null || offsetX != null) {
                computedSpeed = (float) (speed != null ? molangEval.eval(speed) : 0);
                double evaledCount = count != null ? molangEval.eval(count) : 1;
                computedCount = evaledCount < 1 ? 1 : (int) evaledCount;
                computedOffset = new Vec(
                    offsetX != null ? molangEval.eval(offsetX) : 0,
                    offsetY != null ? molangEval.eval(offsetY) : 0,
                    offsetZ != null ? molangEval.eval(offsetZ) : 0
                );
            } else if (velocityX != null) {
                var computedVelocity = new Vec(
                    molangEval.eval(velocityX),
                    molangEval.eval(velocityY),
                    molangEval.eval(velocityZ)
                );
                computedSpeed = (float) computedVelocity.length();
                computedCount = 0;
                computedOffset = computedVelocity.normalize();
            } else {
                computedSpeed = 0;
                computedCount = 1;
                computedOffset = Vec.ZERO;
            }

            try {
                var computedParticle = particle.get();
                var particlePacket = new ParticlePacket(computedParticle, false, false, position, computedOffset, computedSpeed, computedCount);
                for (var viewer : entity.getViewers()) {
                    if (viewer.getSettings().particleSetting() == ClientSettings.ParticleSetting.MINIMAL)
                        continue;
                    viewer.sendPacket(particlePacket);
                }

            } catch (IllegalArgumentException e) {
                logger.error("failed molang eval in particle spawn: {}", e.getMessage());
            }
        }
    }

    private void loadParticleData(@NotNull CompoundBinaryTag data) throws IllegalArgumentException {
        var keys = data.keySet();

        // Particle
        var particleName = data.getString("particle");
        Check.argCondition(particleName.isEmpty(), "Missing particle name");
        var rawParticle = Particle.fromKey(particleName);
        Check.argCondition(rawParticle == null, "Unknown particle: " + particleName);
        readTypedParticleData(rawParticle, data);

        // Rate
        if (keys.contains("rate")) {
            if (!(data.get("rate") instanceof NumberBinaryTag rateTag))
                throw new IllegalArgumentException("rate: expected number, got " + data.get("rate").getClass().getSimpleName());
            Check.argCondition(rateTag.doubleValue() <= 0, "rate: must be positive");
            this.rate = Math.min(rateTag.doubleValue(), MAX_PARTICLES_PER_SECOND / 20.0);
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

        // Active
        active = loadValueScript("active", data.get("active"));

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

    private @Nullable MolangExpr loadValueScript(@NotNull String name, @Nullable BinaryTag tag) {
        if (tag == null) return null;
        if (tag instanceof StringBinaryTag scriptTag) {
            try {
                return MolangExpr.parseOrThrow(scriptTag.value());
            } catch (Throwable e) {
                throw new IllegalArgumentException(name + ": failed to compile script: " + e.getMessage());
            }
        } else if (tag instanceof NumberBinaryTag numberTag) {
            return new MolangExpr.Num(numberTag.doubleValue());
        } else {
            throw new IllegalArgumentException(name + ": expected number or script, got " + tag.getClass().getSimpleName());
        }
    }

    private void readTypedParticleData(@NotNull Particle rawParticle, @NotNull CompoundBinaryTag data) throws IllegalArgumentException {
        switch (rawParticle) {
            case Particle.Block blockParticle when data.get("block") instanceof StringBinaryTag blockName -> {
                try {
                    particle = () -> blockParticle.withBlock(ArgumentBlockState.staticParse(blockName.value()));
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalArgumentException("Invalid block state: " + e.getMessage());
                }
            }
            case
                Particle.BlockMarker blockMarkerParticle when data.get("block") instanceof StringBinaryTag blockName -> {
                try {
                    particle = () -> blockMarkerParticle.withBlock(ArgumentBlockState.staticParse(blockName.value()));
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalArgumentException("Invalid block state: " + e.getMessage());
                }
            }
            case Particle.Dust dustParticle -> {
                MolangExpr red, green, blue;
                if (data.keySet().contains("color")) {
                    var colorTag = assertVecTag("color", data.get("color"));
                    red = loadValueScript("color.r", colorTag.get(0));
                    green = loadValueScript("color.g", colorTag.get(1));
                    blue = loadValueScript("color.b", colorTag.get(2));
                } else red = green = blue = null;
                var scale = loadValueScript("scale", data.get("scale"));
                particle = () -> {
                    var p = dustParticle;
                    if (red != null && green != null && blue != null) p = p.withColor(new Color(
                        (int) (molangEval.eval(red) * 255.),
                        (int) (molangEval.eval(green) * 255.),
                        (int) (molangEval.eval(blue) * 255.)
                    ));
                    if (scale != null) p = p.withScale((float) molangEval.eval(scale));
                    return p;
                };
            }
            case Particle.DustColorTransition dustColorTransitionParticle -> {
                MolangExpr red, green, blue;
                if (data.keySet().contains("color")) {
                    var colorTag = assertVecTag("color", data.get("color"));
                    red = loadValueScript("color.r", colorTag.get(0));
                    green = loadValueScript("color.g", colorTag.get(1));
                    blue = loadValueScript("color.b", colorTag.get(2));
                } else red = green = blue = null;
                MolangExpr tRed, tGreen, tBlue;
                if (data.keySet().contains("transition")) {
                    var transitionTag = assertVecTag("transition", data.get("transition"));
                    tRed = loadValueScript("transition.r", transitionTag.get(0));
                    tGreen = loadValueScript("transition.g", transitionTag.get(1));
                    tBlue = loadValueScript("transition.b", transitionTag.get(2));
                } else tRed = tGreen = tBlue = null;
                var scale = loadValueScript("scale", data.get("scale"));
                particle = () -> {
                    var p = dustColorTransitionParticle;
                    if (red != null && green != null && blue != null) p = p.withColor(new Color(
                        (int) (molangEval.eval(red) * 255.),
                        (int) (molangEval.eval(green) * 255.),
                        (int) (molangEval.eval(blue) * 255.)
                    ));
                    if (tRed != null && tGreen != null && tBlue != null) p = p.withTransitionColor(new Color(
                        (int) (molangEval.eval(tRed) * 255.),
                        (int) (molangEval.eval(tGreen) * 255.),
                        (int) (molangEval.eval(tBlue) * 255.)
                    ));
                    if (scale != null) p = p.withScale((float) molangEval.eval(scale));
                    return p;
                };
            }
            case Particle.DustPillar dustPillarParticle when data.get("block") instanceof StringBinaryTag blockName -> {
                try {
                    particle = () -> dustPillarParticle.withBlock(ArgumentBlockState.staticParse(blockName.value()));
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalArgumentException("Invalid block state: " + e.getMessage());
                }
            }
            // EntityEffect
            case
                Particle.FallingDust fallingDustParticle when data.get("block") instanceof StringBinaryTag blockName -> {
                try {
                    particle = () -> fallingDustParticle.withBlock(ArgumentBlockState.staticParse(blockName.value()));
                } catch (ArgumentSyntaxException e) {
                    throw new IllegalArgumentException("Invalid block state: " + e.getMessage());
                }
            }
            case Particle.Item itemParticle when data.get("item") instanceof StringBinaryTag itemName -> {
                var material = Material.fromKey(itemName.value());
                Check.argCondition(material == null, "Unknown item: " + itemName);
                particle = () -> itemParticle.withItem(ItemStack.of(material));
                //todo should support custom model data and maybe other components.
            }
            // SculkCharge, Shriek, Vibration
            case Particle.TintedLeaves itemParticle when data.get("color") instanceof ListBinaryTag colorTag -> {
                var red = loadValueScript("color.r", colorTag.get(0));
                var green = loadValueScript("color.g", colorTag.get(1));
                var blue = loadValueScript("color.b", colorTag.get(2));
                var alpha = loadValueScript("color.a", colorTag.get(3));
                particle = () -> itemParticle.withColor(new AlphaColor(
                    alpha != null ? (int) (molangEval.eval(alpha)) : 255,
                    red != null ? (int) (molangEval.eval(red)) : 255,
                    green != null ? (int) (molangEval.eval(green)) : 255,
                    blue != null ? (int) (molangEval.eval(blue)) : 255
                ));
            }
            default -> {
                particle = () -> rawParticle;
            } // No data or unsupported
        }
    }


    private static class Variables implements MolangValue.Holder {
        public double age;
        public double lifetime;
        public double random1;
        public double random2;
        public double random3;
        public double random4;
        public double particleRandom1;
        public double particleRandom2;
        public double particleRandom3;
        public double particleRandom4;

        @Override
        public @NotNull MolangValue get(@NotNull String field) {
            return switch (field) {
                case "age" -> new MolangValue.Num(age);
                case "lifetime" -> new MolangValue.Num(lifetime);
                case "random_1" -> new MolangValue.Num(random1);
                case "random_2" -> new MolangValue.Num(random2);
                case "random_3" -> new MolangValue.Num(random3);
                case "random_4" -> new MolangValue.Num(random4);
                case "particle_random_1" -> new MolangValue.Num(particleRandom1);
                case "particle_random_2" -> new MolangValue.Num(particleRandom2);
                case "particle_random_3" -> new MolangValue.Num(particleRandom3);
                case "particle_random_4" -> new MolangValue.Num(particleRandom4);
                default -> MolangValue.NIL;
            };
        }
    }

    private static class Queries implements MolangValue.Holder {
        public static final Queries INSTANCE = new Queries();

        private Queries() {
        }

        private static double hsbToRed(double hue, double saturation, double brightness) {
            if (saturation == 0) return brightness;
            double h = (hue - Math.floor(hue)) * 6.0f;
            double f = h - Math.floor(h);
            return switch ((int) h) {
                case 1 -> brightness * (1.0f - saturation * f);
                case 2, 3 -> brightness * (1.0f - saturation);
                case 4 -> brightness * (1.0f - (saturation * (1.0f - f)));
                default -> brightness;
            };
        }

        private static double hsbToGreen(double hue, double saturation, double brightness) {
            if (saturation == 0) return brightness;
            double h = (hue - Math.floor(hue)) * 6.0f;
            double f = h - Math.floor(h);
            return switch ((int) h) {
                case 0 -> brightness * (1.0f - (saturation * (1.0f - f)));
                case 3 -> brightness * (1.0f - saturation * f);
                case 4, 5 -> brightness * (1.0f - saturation);
                default -> brightness;
            };
        }

        private static double hsbToBlue(double hue, double saturation, double brightness) {
            if (saturation == 0) return brightness;
            double h = (hue - Math.floor(hue)) * 6.0f;
            double f = h - Math.floor(h);
            return switch ((int) h) {
                case 0, 1 -> brightness * (1.0f - saturation);
                case 2 -> brightness * (1.0f - (saturation * (1.0f - f)));
                case 5 -> brightness * (1.0f - saturation * f);
                default -> brightness;
            };
        }

        private static final MolangValue.Function HSB_TO_RED = (rawArgs) -> {
            double[] args = checkArgs(rawArgs, 3);
            return new MolangValue.Num(hsbToRed(args[0], args[1], args[2]));
        };
        private static final MolangValue.Function HSB_TO_GREEN = (rawArgs) -> {
            double[] args = checkArgs(rawArgs, 3);
            return new MolangValue.Num(hsbToGreen(args[0], args[1], args[2]));
        };
        private static final MolangValue.Function HSB_TO_BLUE = (rawArgs) -> {
            double[] args = checkArgs(rawArgs, 3);
            return new MolangValue.Num(hsbToBlue(args[0], args[1], args[2]));
        };

        @Override
        public @NotNull MolangValue get(@NotNull String field) {
            return switch (field) {
                case "hsb_to_red" -> HSB_TO_RED;
                case "hsb_to_green" -> HSB_TO_GREEN;
                case "hsb_to_blue" -> HSB_TO_BLUE;
                default -> MolangValue.NIL;
            };
        }

        private static double[] checkArgs(@NotNull List<MolangValue> args, int expected) {
            if (args.size() != expected)
                throw new IllegalArgumentException("expected " + expected + " arguments, got: " + args.size());
            double[] result = new double[expected];
            for (int i = 0; i < expected; i++) {
                // TODO: this needs to generate a content error...
                result[i] = args.get(i) instanceof MolangValue.Num(double value) ? value : 0.0;
            }
            return result;
        }
    }
}
