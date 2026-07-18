package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudBar;
import net.hollowcube.common.hud.HudText;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.monitoring.BenchmarkManager;
import net.minestom.server.monitoring.TickMonitor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ServerInfoHud implements HudBar.Module {
    private static final BenchmarkManager BENCHMARK_MANAGER = MinecraftServer.getBenchmarkManager();

    private static final AtomicReference<TickMonitor> LAST_TICK = new AtomicReference<>();

    static {
        MinecraftServer.getGlobalEventHandler()
                .addListener(ServerTickMonitorEvent.class, event -> LAST_TICK.set(event.getTickMonitor()));
    }

    // Just past the right edge of the hotbar (91px half-width), roughly centered on its height.
    private static final int OFFSET_X = 100;
    private static final int OFFSET_Y = -15;

    private long lastUpdate = 0;

    private double lastTickTime;
    private int lastMemoryUsage;

    @Override
    public int cacheKey(@NotNull Player player) {
        long now = System.currentTimeMillis();
        return Objects.hash(now - lastUpdate > 1000);
    }

    @Override
    public @NotNull Component render(@NotNull Player player) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 1000) {
            var tickMonitor = LAST_TICK.get();
            if (tickMonitor == null) return Component.empty(); // sanity
            lastTickTime = tickMonitor.getTickTime();
            lastMemoryUsage = (int) (BENCHMARK_MANAGER.getUsedMemory() / 1e6);
            lastUpdate = now;
        }

        var text = String.format("%.2f", lastTickTime) + "ms // " + lastMemoryUsage + "MB";
        var world = MapWorld.forPlayer(player);
        if (world != null) {
            text += " // " + world.map().settings().getSize().name().toLowerCase(Locale.ROOT)
                    + "-" + ServerRuntime.getRuntime().size();
        }

        return Component.text()
                .append(Component.text(FontUtil.computeOffset(OFFSET_X)))
                .append(HudText.text(text, HudAnchor.BOTTOM, OFFSET_Y, NamedTextColor.WHITE))
                .append(Component.text(FontUtil.computeOffset(-OFFSET_X - FontUtil.measureText(text))))
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ServerInfoHud;
    }

    @Override
    public int hashCode() {
        return ServerInfoHud.class.hashCode() * 31;
    }
}
