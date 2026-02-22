package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ServerLatencyHud implements ActionBar.Provider {

    private long lastUpdate = 0;

    @Override
    public int cacheKey(@NotNull Player player) {
        long now = System.currentTimeMillis();
        return Objects.hash(now - lastUpdate < 500);
    }

    @Override
    public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
        this.lastUpdate = System.currentTimeMillis();

        builder.offset(250);

        int y = -50;
        for (var otherPlayer : player.getInstance().getPlayers()) {
            if (otherPlayer instanceof MapPlayer mp) {
                int latency = mp.getLatency();
                double avgLatency = mp.averageLatency();

                var text = "%s: %dms (avg %.2fms)".formatted(mp.getUsername(), latency, avgLatency);
                var textWidth = FontUtil.measureText(text);

                builder.pushShadowColor(ShadowColor.none());
                builder.pushColor(FontUtil.computeVerticalOffset(y));
                builder.append(FontUtil.rewrite("anvil_title", text), textWidth);
                builder.popShadowColor();
                builder.popColor();

                builder.tempReset();
                builder.offset(250);

                y += 12;
            }
        }
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
