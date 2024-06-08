package net.hollowcube.mapmaker.map.util;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.configuration.SelectKnownPacksPacket;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    public static void smartAddItemStack(@NotNull Player player, @NotNull ItemStack itemStack) {
        net.hollowcube.terraform.util.PlayerUtil.smartAddItemStack(player, itemStack);
    }

    private static final BoundingBox PLAYER_STANDING_BB = EntityType.PLAYER.registry().boundingBox();

    public static boolean canFit(@NotNull Player player, @NotNull Point position) {
        var instance = player.getInstance();
        var iter = PLAYER_STANDING_BB.getBlocks(position);
        while (iter.hasNext()) {
            var pos = iter.next();
            var blockShape = instance.getBlock(new Vec(pos.x(), pos.y(), pos.z()), Block.Getter.Condition.TYPE).registry().collisionShape();
            boolean hit = blockShape.intersectBox(position.sub(pos.blockX(), pos.blockY(), pos.blockZ()), PLAYER_STANDING_BB);
            if (hit) return false;
        }
        return true;
    }

    public static boolean canMoveTo(@NotNull Player player, @NotNull Point position) {
        var result = CollisionUtils.handlePhysics(
                player.getInstance(), player.getChunk(),
                PLAYER_STANDING_BB, player.getPosition(),
                Vec.fromPoint(position.sub(player.getPosition())),
                null, true
        );
        return !result.collisionX() && !result.collisionY() && !result.collisionZ();
    }

    public static @NotNull CompletableFuture<List<SelectKnownPacksPacket.Entry>> stealKnownPacksFuture(@NotNull Player player) {
        class Holder {
            static Field knownPacksFuture;
        }
        try {
            if (Holder.knownPacksFuture == null) {
                Holder.knownPacksFuture = PlayerConnection.class.getDeclaredField("knownPacksFuture");
                Holder.knownPacksFuture.setAccessible(true);
            }

            return (CompletableFuture<List<SelectKnownPacksPacket.Entry>>) Holder.knownPacksFuture.get(player.getPlayerConnection());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
