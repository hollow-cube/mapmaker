package net.hollowcube.map.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.utils.block.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

@SuppressWarnings("UnstableApiUsage")
public final class PlayerUtil {
    public static final double DEFAULT_PLACE_REACH = 4.5;

    /**
     * The builtin version of this function takes an integer... for some reason.
     */
    public static @Nullable Point getTargetBlock(@NotNull Player player, double maxDistance) {
        var instance = player.getInstance();
        if (instance == null) return null;
        var pos = player.getPosition();

        Iterator<Point> it = new BlockIterator(pos.asVec(), pos.direction(),
                player.getEyeHeight(), maxDistance, false);
        while (it.hasNext()) {
            final Point position = it.next();
            if (!instance.getBlock(position, Block.Getter.Condition.TYPE).isAir()) return position;
        }
        return null;
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
