package net.hollowcube.common.hud;

import net.hollowcube.common.util.FutureUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The carrier for anchored HUD text ({@link HudText}): a single boss bar whose name is the
 * concatenation of every module's content. It must be the player's <b>first</b> boss bar
 * (the shader resolves anchors against the first bar's name origin), so create it before
 * showing any other bar. The bar itself is invisible: the pack blanks the yellow bar sprites.
 */
public final class HudBar {
    private static final Tag<HudBar> TAG = Tag.Transient("hud_bar");

    public static @NotNull HudBar forPlayer(@NotNull Player player) {
        var instance = player.getTag(TAG);
        if (instance == null) {
            instance = new HudBar(player);
            player.setTag(TAG, instance);
        }
        return instance;
    }

    public interface Module {
        /**
         * Content for this module. Every glyph must be anchored ({@link HudText}) and the
         * run must be composed to net-zero advance so modules cannot shift each other.
         */
        @NotNull Component render(@NotNull Player player);

        default int cacheKey(@NotNull Player player) {
            return ThreadLocalRandom.current().nextInt();
        }
    }

    private final Set<Module> modules = new CopyOnWriteArraySet<>();
    private final Player player;
    private final BossBar bar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    private int lastHash = 0;

    private HudBar(@NotNull Player player) {
        this.player = player;

        player.showBossBar(bar);
        player.scheduler().submitTask(this::update);
    }

    public void addModule(@NotNull Module module) {
        FutureUtil.assertTickThreadWarn();
        modules.remove(module); // Remove if already exists (to use latest version always)
        modules.add(module);
    }

    public void removeModule(@NotNull Module module) {
        FutureUtil.assertTickThreadWarn();
        modules.remove(module);
    }

    private @NotNull TaskSchedule update() {
        if (player.getPlayerConnection().getClientState() != ConnectionState.PLAY)
            return TaskSchedule.tick(2);

        int hash = 1;
        for (Module module : modules) {
            hash = 31 * hash + module.cacheKey(player);
            hash = 31 * hash + module.getClass().hashCode();
        }

        if (hash != lastHash) {
            lastHash = hash;

            var name = Component.text();
            for (Module module : modules) {
                name.append(module.render(player));
            }
            bar.name(name.build());
        }

        return TaskSchedule.tick(2);
    }
}
