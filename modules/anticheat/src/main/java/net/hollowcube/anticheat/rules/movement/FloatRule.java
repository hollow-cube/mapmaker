package modules.anticheat.src.main.java.net.hollowcube.anticheat.rules.movement;

import com.google.auto.service.AutoService;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatNotifier;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatRule;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.utils.AntiCheatUtils;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.utils.TagTimer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Checks for players hovering.
 */
@AutoService(AntiCheatRule.class)
public final class FloatRule extends MovementRule {

    private static final int PRECISION = 3; // How many decimal points we should check when doing the comparison.
    private static final int THRESHOLD = 1000; // How long the player should hover before being considered as cheating.
    private static final int MANUAL_CHECK_INTERVAL = 3000; // How often we should check if the player is still hovering if they are not moving.

    private static final Tag<Long> FIRST_HOVER = Tag.<Long>Transient("anticheat:float/first_hover").defaultValue(-1L);
    private static final TagTimer LAST_CHECK = new TagTimer("anticheat:float/last_check", MANUAL_CHECK_INTERVAL);
    private static final TagTimer LAST_NOTIFICATION = new TagTimer("anticheat:float/last_notification", 10000);

    @Override
    public void onInitialize(@NotNull GlobalEventHandler events, @NotNull AntiCheatNotifier notifier) {
        super.onInitialize(events, notifier);
        events.addListener(PlayerTickEvent.class, event -> onPlayerTick(event, notifier));
    }

    @Override
    protected void onPlayerMove(@NotNull PlayerMoveEvent event, @NotNull AntiCheatNotifier notifier) {
        var player = event.getPlayer();
        var oldPos = player.getPosition();
        var newPos = event.getNewPosition();

        LAST_CHECK.update(player);

        if (!AntiCheatUtils.areSimilar(oldPos.y(), newPos.y(), PRECISION)) {
            player.removeTag(FIRST_HOVER);
            return;
        }

        checkHovering(player, notifier);
    }

    private void onPlayerTick(@NotNull PlayerTickEvent event, @NotNull AntiCheatNotifier notifier) {
        if (LAST_CHECK.test(event.getPlayer())) {
            checkHovering(event.getPlayer(), notifier);
        }
    }

    private void checkHovering(@NotNull Player player, @NotNull AntiCheatNotifier notifier) {
        if (isTouching(player, block -> !block.isLiquid() && !block.isAir())) {
            player.removeTag(FIRST_HOVER);
            return;
        }

        var firstHover = player.getTag(FIRST_HOVER);
        if (firstHover == -1) {
            player.setTag(FIRST_HOVER, System.currentTimeMillis());
        } else if (System.currentTimeMillis() - firstHover >= THRESHOLD && LAST_NOTIFICATION.test(player)) {
            notifier.sendNotification(player, "float", "Player is hovering.");
        }
    }

    private static boolean isTouching(Player player, Predicate<Block> checker) {
        var box = player.getBoundingBox();
        var pos = player.getPosition();

        var minX = (int) Math.floor(box.minX() + pos.x());
        var maxX = (int) Math.ceil(box.maxX() + pos.x());
        var minZ = (int) Math.floor(box.minZ() + pos.z());
        var maxZ = (int) Math.ceil(box.maxZ() + pos.z());
        var minY = (int) Math.floor(box.minY() + pos.y());

        var standingOnValidBlocks = false;

        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                var block = player.getInstance().getBlock(x, minY - 1, z, Block.Getter.Condition.TYPE);
                if (block == null) continue;

                var relativePoint = new Vec(
                        x - (int) pos.x(),
                        minY - 1 - (int) pos.y(),
                        z - (int) pos.z()
                );

                if (block.registry().collisionShape().intersectBox(relativePoint, box)) {
                    if (!checker.test(block)) {
                        return false;
                    } else {
                        standingOnValidBlocks = true;
                    }
                }
            }
        }

        return standingOnValidBlocks;
    }
}
