package modules.anticheat.src.main.java.net.hollowcube.anticheat.rules.movement;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Unit;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatNotifier;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatRule;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * Checks for players sending multiple move packets in a short period of time.
 */
@AutoService(AntiCheatRule.class)
public final class BlinkRule implements AntiCheatRule {

    private static final int THRESHOLD = 1;

    private static final Tag<Integer> MOVEMENTS = Tag.<Integer>Transient("anticheat:blink/movements").defaultValue(0);
    private static final Tag<Unit> HAS_NOTIFIED = Tag.Transient("anticheat:blink/has_notified");

    @Override
    public void onInitialize(@NotNull GlobalEventHandler events, @NotNull AntiCheatNotifier notifier) {
        events.addListener(PlayerMoveEvent.class, event -> onPlayerMove(event, notifier));
        events.addListener(PlayerTickEndEvent.class, this::onClientPlayerTick);
    }

    private void onClientPlayerTick(@NotNull PlayerTickEndEvent event) {
        var player = event.getPlayer();
        player.removeTag(MOVEMENTS);
        player.removeTag(HAS_NOTIFIED);
    }

    private void onPlayerMove(@NotNull PlayerMoveEvent event, @NotNull AntiCheatNotifier notifier) {
        var player = event.getPlayer();

        int movements = player.getAndUpdateTag(MOVEMENTS, i -> i + 1);

        if (movements > THRESHOLD && player.hasTag(HAS_NOTIFIED)) {
            notifier.sendNotification(player, "blink", "Player has sent multiple move packets in a single client tick.");
            player.setTag(HAS_NOTIFIED, Unit.INSTANCE);
        }
    }
}
