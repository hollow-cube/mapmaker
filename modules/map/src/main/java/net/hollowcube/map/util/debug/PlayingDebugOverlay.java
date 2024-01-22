package net.hollowcube.map.util.debug;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayingDebugOverlay implements ActionBar.Provider {
    public static final PlayingDebugOverlay INSTANCE = new PlayingDebugOverlay();

    private PlayingDebugOverlay() {
    }

    @Override
    public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || (world.flags() & MapWorld.FLAG_PLAYING) == 0) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null || !MapHooks.isPlayerPlaying(player)) return;
        
        var sliced = slice(saveState.playState().toString(false));
        builder.offset(-FontUtil.measureText(sliced) / 2).append("line_0", sliced);
    }

    private @NotNull String slice(@NotNull String str) {
        if (str.length() > 200) {
            return str.substring(0, 200) + "...";
        }
        return str;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayingDebugOverlay;
    }

    @Override
    public int hashCode() {
        return PlayingDebugOverlay.class.hashCode();
    }
}
