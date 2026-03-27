package net.hollowcube.mapmaker.runtime.parkour.action.impl.variables;

import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class VariableQueries {

    public static @Nullable Double resolve(String field, @Nullable Player player) {
        if (player == null) return null;
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return null;
        var state = world.getPlayerState(player);
        var best = world.getPlayerBestPlaytime(player);

        return switch (field) {
            case "playtime" -> state instanceof ParkourState.AnyPlaying playing ? (double) playing.saveState().getRealPlaytime() / 1000.0 : null;
            case "best_playtime" -> best != null ? (double) best / 1000.0 : null;
            default -> null;
        };
    }
}
