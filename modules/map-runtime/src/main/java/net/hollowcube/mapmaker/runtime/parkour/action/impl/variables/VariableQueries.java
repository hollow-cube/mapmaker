package net.hollowcube.mapmaker.runtime.parkour.action.impl.variables;

import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.molang.MolangEnvironment;
import net.hollowcube.molang.MolangValue;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class VariableQueries {

    public record Context(Player player, @Nullable VariableStorage variables) {
    }

    public static final MolangEnvironment<Context> ENVIRONMENT = MolangEnvironment.<Context>builder()
            .query(q -> q
                    .optionalNumber("playtime", ctx -> playtime(ctx.player()))
                    .optionalNumber("best_playtime", ctx -> bestPlaytime(ctx.player())))
            .variables(v -> v.dynamic((ctx, name, _) -> new MolangValue.Num(
                    ctx.variables() != null ? ctx.variables().getOrDefault(name, 0.0) : 0.0)))
            .build();

    private static @Nullable Double playtime(Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return null;
        return switch (world.getPlayerState(player)) {
            case ParkourState.AnyPlaying playing -> (double) playing.saveState().getEffectivePlaytime();
            case ParkourState.Finished finished -> (double) finished.saveState().getEffectivePlaytime();
            default -> null;
        };
    }

    private static @Nullable Double bestPlaytime(Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return null;
        var best = world.getPlayerBestPlaytime(player);
        return best != null ? (double) best : null;
    }
}
