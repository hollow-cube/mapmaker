package net.hollowcube.mapmaker.runtime.parkour.action.impl.velocity;

import net.hollowcube.common.math.relative.RelativeField;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.runtime.parkour.action.MolangExpression;
import net.hollowcube.molang.eval.MolangEvaluator;
import net.hollowcube.molang.runtime.ContentError;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VelocityModifiers {

    public record DirectionPower(RelativeField yaw, RelativeField pitch, double power) implements VelocityModifier {

        public static final double DEFAULT_POWER = 25;
        public static final DirectionPower DEFAULT = new DirectionPower(RelativeField.ORIGIN, RelativeField.ORIGIN, DEFAULT_POWER);
        public static final StructCodec<DirectionPower> CODEC = StructCodec.struct(
            "yaw", RelativeField.CODEC.optional(RelativeField.ORIGIN), DirectionPower::yaw,
            "pitch", RelativeField.CODEC.optional(RelativeField.ORIGIN), DirectionPower::pitch,
            "power", Codec.DOUBLE.optional(DEFAULT_POWER), DirectionPower::power,
            DirectionPower::new
        );

        @Override
        public Vec get(Player player) {
            var pos = player.getPosition();
            var yaw = this.yaw.resolve(pos.yaw());
            var pitch = this.pitch.resolve(pos.pitch());

            var xz = Math.cos(Math.toRadians(pitch));
            return new Vec(
                (-xz * Math.sin(Math.toRadians(yaw))) * this.power,
                this.power,
                (xz * Math.cos(Math.toRadians(yaw))) * this.power
            );
        }

        public DirectionPower withYaw(RelativeField yaw) {
            return new DirectionPower(yaw, this.pitch, this.power);
        }

        public DirectionPower withPitch(RelativeField pitch) {
            return new DirectionPower(this.yaw, pitch, this.power);
        }

        public DirectionPower withPower(double power) {
            return new DirectionPower(this.yaw, this.pitch, power);
        }

    }

    public record Molang(MolangExpression dx, MolangExpression dy, MolangExpression dz) implements VelocityModifier {

        public static final StructCodec<Molang> CODEC = StructCodec.struct(
            "dx", MolangExpression.CODEC, Molang::dx,
            "dy", MolangExpression.CODEC, Molang::dy,
            "dz", MolangExpression.CODEC, Molang::dz,
            Molang::new
        );

        private static final MolangResolver<Player> QUERIES = new MolangResolver<>(Molang::resolveQuery);
        private static final MolangResolver<Player> VARIABLES = new MolangResolver<>(Molang::resolveVariable);
        private static final MolangEvaluator EVALUATOR = new MolangEvaluator(Map.of(
            "query", QUERIES,
            "q", QUERIES,
            "variable", VARIABLES,
            "v", VARIABLES
        ));

        @Override
        public @Nullable Vec get(Player player) {
            QUERIES.setContext(player);
            VARIABLES.setContext(player);

            var errors = new ArrayList<ContentError>();
            var dx = evaluate(errors, player, this.dx);
            var dy = evaluate(errors, player, this.dy);
            var dz = evaluate(errors, player, this.dz);

            var world = MapWorld.forPlayer(player);
            if (world != null && !world.map().isPublished() && !errors.isEmpty()) {
                var error = errors.stream().map(ContentError::message).collect(Collectors.joining("\n"));
                player.sendMessage(Component.text("Errors evaluating velocity expression:\n" + error));
            }

            QUERIES.setContext(null);
            VARIABLES.setContext(null);

            if (dx != null && dy != null && dz != null) {
                return new Vec(dx, dy, dz);
            }

            return null;
        }

        private static @Nullable Double evaluate(
            List<ContentError> errors, Player player, MolangExpression expression) {
            var parsed = expression.parsed();
            var error = expression.error();

            if (error != null) {
                errors.add(new ContentError(error.getMessage()));
                return null;
            }

            if (parsed == null) {
                errors.add(new ContentError("Unknown error parsing expression."));
                return null;
            }

            try {
                return EVALUATOR.eval(parsed);
            } catch (ArithmeticException exception) {
                errors.add(new ContentError(exception.getMessage()));
            } catch (Exception exception) {
                ExceptionReporter.reportException(exception, player);
                errors.add(new ContentError("Internal Server Error, please report to administrators if persistent."));
            } finally {
                errors.addAll(EVALUATOR.getErrors());
            }

            return null;
        }

        private static @Nullable Double resolveQuery(String field, @Nullable Player player) {
            if (player == null) return null;
            return switch (field) {
                case "isSneaking" -> player.isSneaking() ? 1.0 : 0.0;
                case "isSwimming" -> player.getPose() == EntityPose.SWIMMING ? 1.0 : 0.0;
                case "isSprinting" -> player.isSprinting() ? 1.0 : 0.0;
                case "isGliding" -> player.isFlyingWithElytra() ? 1.0 : 0.0;
                default -> null;
            };
        }

        private static @Nullable Double resolveVariable(String field, @Nullable Player player) {
            if (player == null) return null;
            return switch (field) {
                case "x" -> player.getPosition().x();
                case "y" -> player.getPosition().y();
                case "z" -> player.getPosition().z();
                case "dx" -> player.getVelocity().x();
                case "dy" -> player.getVelocity().y();
                case "dz" -> player.getVelocity().z();
                case "yaw" -> (double) player.getPosition().yaw();
                case "pitch" -> (double) player.getPosition().pitch();
                default -> null;
            };
        }

    }
}
