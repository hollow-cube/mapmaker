package modules.anticheat.src.main.java.net.hollowcube.anticheat.rules.generic;

import com.google.auto.service.AutoService;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatNotifier;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatRule;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.utils.TagTimer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerTickEndEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * Checks if a player sends an end tick too often
 */
@AutoService(AntiCheatRule.class)
public final class TimerRule implements AntiCheatRule {

    private static final int THRESHOLD = 33; // ~1 / 3 a tick
    private static final int SAMPLES = 10;

    private static final Tag<Entry> LAST_TICKS = Tag.<Entry>Transient("anticheat:timer/last_ticks").defaultValue(Entry::empty);
    private static final TagTimer LAST_NOTIFICATION = new TagTimer("anticheat:timer/last_notification", 10000);

    @Override
    public void onInitialize(@NotNull GlobalEventHandler events, @NotNull AntiCheatNotifier notifier) {
        events.addListener(PlayerTickEndEvent.class, event -> onClientPlayerTick(event, notifier));
    }

    private void onClientPlayerTick(@NotNull PlayerTickEndEvent event, @NotNull AntiCheatNotifier notifier) {
        var player = event.getPlayer();
        var lastTicks = player.getAndUpdateTag(LAST_TICKS, value -> value.add(System.currentTimeMillis()));

        if (lastTicks.size() < SAMPLES) return;
        var avg = lastTicks.avg();
        if (avg < THRESHOLD && LAST_NOTIFICATION.test(player)) {
            notifier.sendNotification(player, "timer", "Player is sending ticks too often.");
        }
    }

    private record Entry(
            int size,
            long[] tickDiffs,
            long lastTick
    ) {

        public static Entry empty() {
            return new Entry(0, new long[SAMPLES], -1);
        }

        public Entry add(long time) {
            if (this.lastTick != -1) {
                // move 1-9 to 0-8
                System.arraycopy(tickDiffs, 1, tickDiffs, 0, tickDiffs.length - 1);
                // add the new tick
                tickDiffs[tickDiffs.length - 1] = time - lastTick;
            }
            return new Entry(
                    Math.min(size + 1, tickDiffs.length),
                    tickDiffs,
                    time
            );
        }

        public long avg() {
            long total = 0;
            for (long diff : tickDiffs) {
                total += diff;
            }
            return total / size;
        }
    }
}
