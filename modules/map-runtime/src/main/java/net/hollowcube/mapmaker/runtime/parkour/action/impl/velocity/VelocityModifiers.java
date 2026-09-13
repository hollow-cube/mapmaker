package net.hollowcube.mapmaker.runtime.parkour.action.impl.velocity;

import net.hollowcube.common.math.relative.RelativeField;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.runtime.parkour.action.MolangExpression;
import net.hollowcube.molang.MolangEnvironment;
import net.hollowcube.molang.MolangState;
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

    public record Molang(
        MolangExpression<Player> dx,
        MolangExpression<Player> dy,
        MolangExpression<Player> dz
    ) implements VelocityModifier {

        private static final MolangEnvironment<Player> ENVIRONMENT = MolangEnvironment.<Player>builder()
            .query(q -> q
                .bool("isSneaking", Player::isSneaking)
                .bool("isSwimming", player -> player.getPose() == EntityPose.SWIMMING)
                .bool("isSprinting", Player::isSprinting)
                .bool("isGliding", Player::isFlyingWithElytra))
            .variables(v -> v
                .number("x", player -> player.getPosition().x())
                .number("y", player -> player.getPosition().y())
                .number("z", player -> player.getPosition().z())
                .number("dx", player -> player.getVelocity().x())
                .number("dy", player -> player.getVelocity().y())
                .number("dz", player -> player.getVelocity().z())
                .number("yaw", player -> player.getPosition().yaw())
                .number("pitch", player -> player.getPosition().pitch()))
            .build();

        public static final StructCodec<Molang> CODEC = StructCodec.struct(
            "dx", MolangExpression.codec(ENVIRONMENT), Molang::dx,
            "dy", MolangExpression.codec(ENVIRONMENT), Molang::dy,
            "dz", MolangExpression.codec(ENVIRONMENT), Molang::dz,
            Molang::new
        );

        @Override
        public @Nullable Vec get(Player player) {
            var errors = new ArrayList<ContentError>();
            var dx = evaluate(errors, player, this.dx);
            var dy = evaluate(errors, player, this.dy);
            var dz = evaluate(errors, player, this.dz);

            var world = MapWorld.forPlayer(player);
            if (world != null && !world.map().isPublished() && !errors.isEmpty()) {
                var error = errors.stream().map(ContentError::toString).collect(Collectors.joining("\n"));
                player.sendMessage(Component.text("Errors evaluating velocity expression:\n" + error));
            }

            if (dx != null && dy != null && dz != null) {
                return new Vec(dx, dy, dz);
            }

            return null;
        }

        private static @Nullable Double evaluate(
            List<ContentError> errors, Player player, MolangExpression<Player> expression) {
            var program = expression.program();
            var error = expression.error();

            if (error != null) {
                errors.add(new ContentError(error.getMessage()));
                return null;
            }

            if (program == null) {
                errors.add(new ContentError("Unknown error parsing expression."));
                return null;
            }

            var state = new MolangState();
            try {
                return program.eval(state, player);
            } catch (Exception exception) {
                ExceptionReporter.reportException(exception, player);
                errors.add(new ContentError("Internal Server Error, please report to administrators if persistent."));
            } finally {
                errors.addAll(state.getErrors());
            }

            return null;
        }

    }
}
