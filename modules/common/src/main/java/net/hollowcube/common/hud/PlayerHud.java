package net.hollowcube.common.hud;

import net.hollowcube.common.util.FutureUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/// The carrier for anchored HUD text ([HudText]): a single boss bar whose name is the
/// concatenation of every module's content. It must be the player's **first** boss bar
/// (the shader resolves anchors against the first bar's name origin), so create it before
/// showing any other bar. The bar itself is invisible: the pack blanks the yellow bar sprites.
public final class PlayerHud {
    private static final Tag<PlayerHud> TAG = Tag.Transient("hud");

    static {
        // TODO: fix in minestom. boss bars are wiped during config but the players remain as a viewer which is wrong.
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            var hud = event.getPlayer().getTag(TAG);
            if (hud != null) hud.needsReshow = true;
        });
    }

    public static PlayerHud forPlayer(Player player) {
        var instance = player.getTag(TAG);
        if (instance == null) {
            instance = new PlayerHud(player);
            player.setTag(TAG, instance);
        }
        return instance;
    }

    public static Module staticModule(HudNode.Anchored content) {
        return _ -> content;
    }

    public interface Module {
        @Nullable HudNode.Anchored render(Player player);
    }

    private static final long DEFAULT_MESSAGE_DURATION = 1000;
    private static final int MESSAGE_OFFSET_Y = -72;

    private final Set<Module> modules = new CopyOnWriteArraySet<>();
    private final Player player;
    private final BossBar bar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    private List<HudNode.Anchored> lastRender = List.of();
    private HudNode.@Nullable Anchored message;
    private long messageUntil;
    private volatile boolean needsReshow = false;

    private PlayerHud(Player player) {
        this.player = player;

        player.showBossBar(bar);
        player.scheduler().submitTask(this::update);
    }

    public void addModule(Module module) {
        FutureUtil.assertTickThreadWarn();
        modules.remove(module); // Remove if already exists (to use latest version always)
        modules.add(module);
    }

    public void removeModule(Module module) {
        FutureUtil.assertTickThreadWarn();
        modules.remove(module);
    }

    public void toggleModule(Module module) {
        FutureUtil.assertTickThreadWarn();
        if (!modules.remove(module)) {
            modules.add(module);
        }
    }

    /// Shows a temporary message above the hotbar.
    /// Immediately replaces any active message.
    public void showMessage(Component message) {
        showMessage(message, DEFAULT_MESSAGE_DURATION);
    }

    /// Shows a message above the hotbar for durationMs.
    /// Immediately replaces any active message.
    public void showMessage(Component message, long durationMs) {
        FutureUtil.assertTickThreadWarn();
        this.message = HudNode.text(message)
            .frame(0, HudNode.Align.CENTER)
            .offset(-1, MESSAGE_OFFSET_Y)
            .anchored(HudAnchor.BOTTOM);
        this.messageUntil = System.currentTimeMillis() + durationMs;
    }

    private TaskSchedule update() {
        if (player.getPlayerConnection().getClientState() != ConnectionState.PLAY)
            return TaskSchedule.tick(2);

        if (needsReshow) {
            needsReshow = false;
            player.hideBossBar(bar);
            player.showBossBar(bar);
        }

        if (message != null && System.currentTimeMillis() >= messageUntil) message = null;

        var render = new ArrayList<HudNode.Anchored>(modules.size() + 1);
        for (Module module : modules) {
            var node = module.render(player);
            if (node != null) render.add(node);
        }
        if (message != null) render.add(message);

        if (!render.equals(lastRender)) {
            lastRender = render;
            bar.name(HudBaker.bake(render));
        }

        return TaskSchedule.tick(2);
    }
}
