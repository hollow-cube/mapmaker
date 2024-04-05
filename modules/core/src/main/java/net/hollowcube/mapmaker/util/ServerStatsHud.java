package net.hollowcube.mapmaker.util;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.monitoring.BenchmarkManager;
import net.minestom.server.monitoring.TickMonitor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class ServerStatsHud implements ActionBar.Provider {
    private static final BenchmarkManager BENCHMARK_MANAGER = MinecraftServer.getBenchmarkManager();

    private static final AtomicReference<TickMonitor> LAST_TICK = new AtomicReference<>();

    static {
        MinecraftServer.getGlobalEventHandler()
                .addListener(ServerTickMonitorEvent.class, event -> LAST_TICK.set(event.getTickMonitor()));
    }

    private long lastUpdate = 0;

    private double lastTickTime;
    private int lastMemoryUsage;

    @Override
    public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 1000) {
            var tickMonitor = LAST_TICK.get();
            if (tickMonitor == null) return; // sanity
            lastTickTime = tickMonitor.getTickTime();
            lastMemoryUsage = (int) (BENCHMARK_MANAGER.getUsedMemory() / 1e6);
            lastUpdate = now;
        }

        builder.offset(125);
        var text = String.format("%.2f", lastTickTime) + "ms // " + lastMemoryUsage + "MB";
        builder.append(FontUtil.rewrite("line_2", text), FontUtil.measureText(text));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ServerStatsHud;
    }

    @Override
    public int hashCode() {
        return ServerStatsHud.class.hashCode() * 31;
    }
}
