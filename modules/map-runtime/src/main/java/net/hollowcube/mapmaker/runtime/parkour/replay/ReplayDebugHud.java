package net.hollowcube.mapmaker.runtime.parkour.replay;

import dev.hollowcube.replay.ReplayRecorder;
import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudNode;
import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.map.util.ServerInfoHud;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;

/// Shows how much the viewer's own run has recorded so far, directly above [ServerInfoHud].
///
/// This lives beside [ReplayManager] rather than with the other debug huds because the recording
/// stats never leave the replay module; map-core cannot see them.
public class ReplayDebugHud implements PlayerHud.Module {
    private static final int LINE_GAP = ServerInfoHud.LINE_GAP;
    private static final int ROW_HEIGHT = FontUtil.DEFAULT_HEIGHT + LINE_GAP;

    @Override
    public HudNode.Anchored render(Player player) {
        var lines = new ArrayList<HudNode>();
        var stats = stats(player);
        if (stats == null) {
            // An empty overlay would be indistinguishable from a toggle that did nothing, and "not
            // recording" is itself the answer being asked for most of the time.
            lines.add(HudNode.text("replay: not recording"));
        } else {
            lines.add(HudNode.text("replay " + formatBytes(stats.bytes()) + " // " + stats.chunks() + " chunks"));
            lines.add(HudNode.text(stats.events() + " events // " + stats.chunkEvents() + " in chunk"));
            lines.add(HudNode.text("tick " + stats.tick()));
        }

        // A vstack grows downward from its offset, so the whole thing is lifted by its own height
        // to land on top of the server info rows rather than through them.
        return HudNode.vstack(LINE_GAP, HudNode.Align.LEFT, lines)
            .offset(ServerInfoHud.OFFSET_X, ServerInfoHud.OFFSET_Y - lines.size() * ROW_HEIGHT)
            .anchored(HudAnchor.BOTTOM);
    }

    private static ReplayRecorder.@Nullable Stats stats(Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        return world == null ? null : world.replayManager().stats(player);
    }

    static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1fkB", bytes / 1024d);
        return String.format(Locale.ROOT, "%.2fMB", bytes / (1024d * 1024d));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReplayDebugHud;
    }

    @Override
    public int hashCode() {
        return ReplayDebugHud.class.hashCode() * 31;
    }
}
