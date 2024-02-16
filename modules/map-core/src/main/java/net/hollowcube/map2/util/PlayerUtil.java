package net.hollowcube.map2.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public final class PlayerUtil {
    public static final double DEFAULT_PLACE_REACH = 4.5;

    /**
     * The builtin version of this function takes an integer... for some reason.
     */
    public static @Nullable Point getTargetBlock(@NotNull Player player, double maxDistance) {
        return net.hollowcube.terraform.util.PlayerUtil.getTargetBlock(player, maxDistance);
    }

    public static void swingHand(@NotNull Player player, @NotNull Player.Hand hand, boolean includeSelf) {
        if (hand == Player.Hand.MAIN) {
            player.swingMainHand(); // Sends only to viewers
            if (includeSelf)
                player.sendPacket(new EntityAnimationPacket(player.getEntityId(), EntityAnimationPacket.Animation.SWING_MAIN_ARM));
        } else {
            player.swingOffHand();
            if (includeSelf)
                player.sendPacket(new EntityAnimationPacket(player.getEntityId(), EntityAnimationPacket.Animation.SWING_OFF_HAND));
        }
    }
}
