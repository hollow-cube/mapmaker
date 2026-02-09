package net.hollowcube.mapmaker.runtime.parkour.action.impl.velocity;

import net.minestom.server.codec.Codec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.Either;
import org.jetbrains.annotations.Nullable;

public sealed interface VelocityModifier permits VelocityModifiers.DirectionPower, VelocityModifiers.Molang {

    double MAX_VELOCITY = 4096;
    Codec<VelocityModifier> CODEC = Codec.EitherStruct(VelocityModifiers.Molang.CODEC, VelocityModifiers.DirectionPower.CODEC)
        .transform(
            either -> either.unify(it -> it, it -> it),
            velocity -> switch (velocity) {
                case VelocityModifiers.Molang molang -> Either.left(molang);
                case VelocityModifiers.DirectionPower directionPower -> Either.right(directionPower);
            }
        );

    @Nullable Vec get(Player player);

    default void apply(Player player) {
        var velocity = this.get(player);
        if (velocity == null) return;

        player.setVelocity(new Vec(
            Math.min(Math.max(velocity.x(), -MAX_VELOCITY), MAX_VELOCITY),
            Math.min(Math.max(velocity.y(), -MAX_VELOCITY), MAX_VELOCITY),
            Math.min(Math.max(velocity.z(), -MAX_VELOCITY), MAX_VELOCITY)
        ));
    }

}
