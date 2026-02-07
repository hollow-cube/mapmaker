package net.hollowcube.mapmaker.runtime.parkour.marker.bouncepad;

import net.hollowcube.common.util.Either;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.hollowcube.molang.MolangExpr;
import net.hollowcube.molang.eval.MolangEvaluator;
import net.hollowcube.molang.eval.MolangValue;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

// TODO: Remove this class entirely and make it a status plate action (not checkpoint, would be a strange checkpoint action)
public sealed interface BouncePadData extends DebugCommand.BlockDebug {

    TagCooldown BOUNCE_PAD_APPLY_COOLDOWN = new TagCooldown("mapmaker:bounce_pad_plate_cooldown", 250);

    double DEFAULT_POWER = 25;
    double MAX_VELOCITY = 4096;

    // Will try to parse the molang format first and then will go to legacy
    StructCodec<BouncePadData> LEGACY_CODEC = StructCodec.struct(
            "power", Codec.DOUBLE.optional(DEFAULT_POWER), data -> data instanceof Simple(
                    var power
            ) ? power : DEFAULT_POWER,
            "legacy_cylone_mode", Codec.BOOLEAN.optional(false), data -> data instanceof Cylone,
            (power, legacyCylone) -> legacyCylone ? new Cylone() : new Simple(power));
    Codec<BouncePadData> CODEC = ExtraCodecs.either(Molang.CODEC, LEGACY_CODEC).transform(
            either -> either.map(it -> it, it -> it),
            data -> data instanceof Molang molang ? Either.left(molang) : Either.right(data));

    @Nullable Vec getVelocity(@NotNull Player player);

    default void applyVelocity(@NotNull Player player) {
        if (!BOUNCE_PAD_APPLY_COOLDOWN.test(player)) return;

        var newVelocity = this.getVelocity(player);
        if (newVelocity == null) return;
        player.setVelocity(new Vec(
            Math.min(Math.max(newVelocity.x(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY),
            Math.min(Math.max(newVelocity.y(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY),
            Math.min(Math.max(newVelocity.z(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY)
        ));
    }

    default void onUpdate(@Nullable Player player) {

    }

    record Simple(double power) implements BouncePadData {

        @Override
        public @NotNull Vec getVelocity(@NotNull Player player) {
            return player.getPosition().direction().withY(1).mul(this.power());
        }

        @Override
        public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
            player.sendMessage("Power: " + this.power());
        }
    }

    record Cylone() implements BouncePadData {

        @Override
        public @NotNull Vec getVelocity(@NotNull Player player) {
            return Vec.fromPoint(player.getPosition().sub(player.getPreviousPosition()).withY(1.45f * 20));
        }

        @Override
        public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
            player.sendMessage("Cyclone: true");
        }
    }

    final class Molang implements BouncePadData {
        private static final Codec<Molang> CODEC = StructCodec.struct(
                "dx", Codec.STRING, data -> data.dxScript,
                "dy", Codec.STRING, data -> data.dyScript,
                "dz", Codec.STRING, data -> data.dzScript,
                Molang::new);

        private final String dxScript;
        private final String dyScript;
        private final String dzScript;

        private final Queries queries = new Queries();
        private final Variables variables = new Variables();
        private final MolangEvaluator molangEval = new MolangEvaluator(Map.of(
                "query", queries,
                "q", queries,
                "variable", variables,
                "v", variables
        ));

        private MolangExpr dx;
        private MolangExpr dy;
        private MolangExpr dz;
        private boolean failedCompilation;

        public Molang(String dxScript, String dyScript, String dzScript) {
            this.dxScript = dxScript;
            this.dyScript = dyScript;
            this.dzScript = dzScript;
        }

        @Override
        public void onUpdate(@Nullable Player player) {
            try {
                this.dx = MolangExpr.parseOrThrow(this.dxScript);
                this.dy = MolangExpr.parseOrThrow(this.dyScript);
                this.dz = MolangExpr.parseOrThrow(this.dzScript);
            } catch (Exception e) {
                this.failedCompilation = true;
                if (player != null) {
                    player.sendMessage("Failed to compile script: " + e.getMessage());
                }
            }
        }

        @Override
        public @Nullable Vec getVelocity(@NotNull Player player) {
            // Sanity check just in case
            if ((this.dx == null || this.dy == null || this.dz == null) && !this.failedCompilation) this.onUpdate(null);
            if (this.failedCompilation) return null;

            try {
                var pos = player.getPosition();
                var vel = player.getVelocity();

                this.queries.player = player;
                this.variables.x = pos.x();
                this.variables.y = pos.y();
                this.variables.z = pos.z();
                this.variables.dx = vel.x();
                this.variables.dy = vel.y();
                this.variables.dz = vel.z();
                this.variables.yaw = pos.yaw();
                this.variables.pitch = pos.pitch();

                return new Vec(
                        evaluate(this.dx, player),
                        evaluate(this.dy, player),
                        evaluate(this.dz, player)
                );
            } catch (ArithmeticException e) {
                logToPlayerInTest(player, "Arithmetic error in Molang expression: " + e.getMessage());
            } catch (Exception e) {
                // Sanity check for unexpected errors, but molang should handle errors gracefully
                ExceptionReporter.reportException(e, player);
            }
            return null;
        }

        private double evaluate(@NotNull MolangExpr expr, @NotNull Player player) {
            var result = molangEval.eval(expr);
            var errors = molangEval.getErrors();
            if (!errors.isEmpty()) {
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Molang evaluation errors for '").append(expr).append("' :\n");
                for (var error : errors) {
                    errorMessage.append(" - ").append(error.message()).append("\n");
                }
                logToPlayerInTest(player, errorMessage.toString());
            }

            return Double.isFinite(result) ? result : 0.0;
        }

        private void logToPlayerInTest(@NotNull Player player, @NotNull String message) {
            // TODO:
//            var world = MapWorld.forPlayerOptional(player);
//            if (world instanceof TestingMapWorld) {
//                player.sendMessage(message);
//            }
        }

        @Override
        public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
            player.sendMessage("Molang: true");
            player.sendMessage("dx: " + this.dxScript);
            player.sendMessage("dy: " + this.dyScript);
            player.sendMessage("dz: " + this.dzScript);
        }

        private static class Variables implements MolangValue.Holder {
            public double x;
            public double y;
            public double z;
            public double dx;
            public double dy;
            public double dz;
            public double yaw;
            public double pitch;

            @Override
            public @NotNull MolangValue get(@NotNull String field) {
                return switch (field) {
                    case "x" -> new MolangValue.Num(x);
                    case "y" -> new MolangValue.Num(y);
                    case "z" -> new MolangValue.Num(z);
                    case "dx" -> new MolangValue.Num(dx);
                    case "dy" -> new MolangValue.Num(dy);
                    case "dz" -> new MolangValue.Num(dz);
                    case "yaw" -> new MolangValue.Num(yaw);
                    case "pitch" -> new MolangValue.Num(pitch);
                    default -> MolangValue.NIL;
                };
            }
        }

        private static class Queries implements MolangValue.Holder {
            public @Nullable Player player;

            public double isSneaking() {
                return player != null && player.getPose() == EntityPose.SNEAKING ? 1 : 0;
            }

            public double isSwimming() {
                return player != null && player.getPose() == EntityPose.SWIMMING ? 1 : 0;
            }

            public double isSprinting() {
                return player != null && player.isSprinting() ? 1 : 0;
            }

            public double isGliding() {
                return player != null && player.isFlyingWithElytra() ? 1 : 0;
            }

            @Override
            public @NotNull MolangValue get(@NotNull String field) {
                return switch (field) {
                    case "isSneaking" -> new MolangValue.Num(isSneaking());
                    case "isSwimming" -> new MolangValue.Num(isSwimming());
                    case "isSprinting" -> new MolangValue.Num(isSprinting());
                    case "isGliding" -> new MolangValue.Num(isGliding());
                    default -> MolangValue.NIL;
                };
            }
        }
    }
}
