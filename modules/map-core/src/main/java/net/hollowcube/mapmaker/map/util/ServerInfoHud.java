package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudNode;
import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.monitoring.TickMonitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class ServerInfoHud implements PlayerHud.Module {
    private static final AtomicReference<TickMonitor> LAST_TICK = new AtomicReference<>();

    static {
        MinecraftServer.getGlobalEventHandler()
            .addListener(ServerTickMonitorEvent.class, event -> LAST_TICK.set(event.getTickMonitor()));
    }

    // Past the right edge of the hotbar (91px half-width), two rows stacked above the bottom edge.
    private static final int OFFSET_X = 130;
    private static final int OFFSET_Y = -35;
    private static final int LINE_GAP = 1; // rows 10px apart

    private long lastUpdate = 0;

    private double lastTickTime;
    private int lastMemoryUsage;

    @Override
    public @Nullable HudNode.Anchored render(@NotNull Player player) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 1000) {
            var tickMonitor = LAST_TICK.get();
            if (tickMonitor == null) return null; // sanity
            lastTickTime = tickMonitor.getTickTime();
            var runtime = Runtime.getRuntime();
            lastMemoryUsage = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1e6);
            lastUpdate = now;
        }

        var lines = new ArrayList<HudNode>();
        lines.add(HudNode.text(String.format("%.2f", lastTickTime) + "ms // " + lastMemoryUsage + "MB"));

        var world = MapWorld.forPlayer(player);
        if (world != null) {
            lines.add(HudNode.text(world.map().settings().getSize().name().toLowerCase(Locale.ROOT)
                                   + "-" + ServerRuntime.getRuntime().size()));
        }

        return HudNode.vstack(LINE_GAP, HudNode.Align.LEFT, lines)
            .offset(OFFSET_X, OFFSET_Y)
            .anchored(HudAnchor.BOTTOM);
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
