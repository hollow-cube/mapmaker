package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudNode;
import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ServerLatencyHud implements PlayerHud.Module {

    private static final int OFFSET_X = -230;
    private static final int OFFSET_Y = -60;
    private static final int LINE_GAP = 3; // rows 12px apart

    @Override
    public HudNode.Anchored render(@NotNull Player player) {
        var lines = new ArrayList<HudNode>();
        for (var otherPlayer : player.getInstance().getPlayers()) {
            if (otherPlayer instanceof MapPlayer mp) {
                lines.add(HudNode.text("%s: %dms (avg %.2fms)".formatted(
                    mp.getUsername(), mp.getLatency(), mp.averageLatency())));
            }
        }

        return HudNode.vstack(LINE_GAP, HudNode.Align.LEFT, lines)
            .offset(OFFSET_X, OFFSET_Y)
            .anchored(HudAnchor.RIGHT);
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
