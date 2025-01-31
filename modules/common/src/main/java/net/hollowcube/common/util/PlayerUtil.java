package net.hollowcube.common.util;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.configuration.SelectKnownPacksPacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.utils.block.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for player-related operations.
 */
public final class PlayerUtil {

    public static final double DEFAULT_PLACEMENT_DISTANCE = 4.5;
    private static final BoundingBox PLAYER_STANDING_BB = EntityType.PLAYER.registry().boundingBox();

    public static @Nullable Point getTargetBlock(@NotNull Player player, double maxDistance, boolean includeLiquids) {
        try {
            var instance = player.getInstance();
            if (instance == null) return null;
            var pos = player.getPosition();

            Iterator<Point> it = new BlockIterator(
                    pos.asVec(), pos.direction(), player.getEyeHeight(), maxDistance, false
            );

            while (it.hasNext()) {
                final Point position = it.next();
                final Block block = instance.getBlock(position, Block.Getter.Condition.TYPE);
                if (!block.isAir() && (includeLiquids || !block.isLiquid())) {
                    return position;
                }
            }
        } catch (NullPointerException e) {
            if (!e.getMessage().contains("Unloaded chunk"))
                throw new RuntimeException(e);
        }
        return null;
    }

    public static void giveItem(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (!player.getInventory().addItemStack(itemStack)) {
            player.setItemInHand(PlayerHand.MAIN, itemStack);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void swing(@NotNull Player player, @NotNull PlayerHand hand, boolean includeSelf) {
        if (hand == PlayerHand.MAIN) player.swingMainHand(includeSelf);
        else player.swingOffHand(includeSelf);
    }

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

    @SuppressWarnings("UnstableApiUsage")
    public static boolean canMoveTo(@NotNull Player player, @NotNull Point position) {
        var result = CollisionUtils.handlePhysics(
                player.getInstance(), player.getChunk(),
                PLAYER_STANDING_BB, player.getPosition(),
                Vec.fromPoint(position.sub(player.getPosition())),
                null, true
        );
        return !result.collisionX() && !result.collisionY() && !result.collisionZ();
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    public static @NotNull CompletableFuture<List<SelectKnownPacksPacket.Entry>> stealKnownPacksFuture(@NotNull Player player) {
        class Holder {
            static Field knownPacksFuture;
        }
        try {
            if (Holder.knownPacksFuture == null) {
                Holder.knownPacksFuture = PlayerConnection.class.getDeclaredField("knownPacksFuture");
                Holder.knownPacksFuture.setAccessible(true);
            }
            Object future = Holder.knownPacksFuture.get(player.getPlayerConnection());
            if (future == null) {
                // There is a race here which could result in the client responding too quickly. In that case we need to re request
                // the known packs. todo: find a better way around this, its really cursed.
                return player.getPlayerConnection().requestKnownPacks(List.of(SelectKnownPacksPacket.MINECRAFT_CORE));
            }
            return (CompletableFuture<List<SelectKnownPacksPacket.Entry>>) future;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
