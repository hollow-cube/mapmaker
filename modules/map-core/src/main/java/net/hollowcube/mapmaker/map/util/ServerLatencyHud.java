package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudBar;
import net.hollowcube.common.hud.HudText;
import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ServerLatencyHud implements HudBar.Module {

    private static final int OFFSET_X = -230;
    private static final int START_Y = -60;
    private static final int LINE_HEIGHT = 12;

    private long lastUpdate = 0;

    @Override
    public int cacheKey(@NotNull Player player) {
        long now = System.currentTimeMillis();
        return Objects.hash(now - lastUpdate < 500);
    }

    @Override
    public @NotNull Component render(@NotNull Player player) {
        this.lastUpdate = System.currentTimeMillis();

        var builder = new FontUIBuilder();
        builder.pushColor(HudText.KILL);

        int y = START_Y;
        for (var otherPlayer : player.getInstance().getPlayers()) {
            if (otherPlayer instanceof MapPlayer mp) {
                int latency = mp.getLatency();
                double avgLatency = mp.averageLatency();

                var text = "%s: %dms (avg %.2fms)".formatted(mp.getUsername(), latency, avgLatency);

                builder.pushShadowColor(HudText.marker(HudAnchor.RIGHT, y, NamedTextColor.WHITE));
                builder.pos(OFFSET_X);
                builder.append(text);
                builder.popShadowColor();

                y += LINE_HEIGHT;
            }
        }

        builder.popColor();
        return builder.build(true);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ServerLatencyHud;
    }

    @Override
    public int hashCode() {
        return ServerLatencyHud.class.hashCode() * 31;
    }
}
