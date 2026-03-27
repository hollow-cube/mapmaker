package net.hollowcube.mapmaker.runtime.parkour.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.minestom.server.entity.Player;

public final class ParkourConditions {

    public static CommandCondition parkourWorld() {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            return ParkourMapWorld.forPlayer(player) != null ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }

    public static CommandCondition anyPlayingOnly() {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            var world = ParkourMapWorld.forPlayer(player);
            if (world == null) return CommandCondition.HIDE;
            return world.getPlayerState(player) instanceof ParkourState.AnyPlaying
                    ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }

    public static CommandCondition spectatorOnly(boolean includeFinished) {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            var world = ParkourMapWorld.forPlayer(player);
            if (world == null) return CommandCondition.HIDE;

            var isSpec = world.getPlayerState(player) instanceof ParkourState.Spectating;
            if (isSpec) return CommandCondition.ALLOW;

            var isFinished = world.getPlayerState(player) instanceof ParkourState.Finished;
            if (includeFinished && isFinished) return CommandCondition.ALLOW;

            return CommandCondition.HIDE;
        };
    }

    public static CommandCondition finishedOnly() {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            var world = ParkourMapWorld.forPlayer(player);
            if (world == null) return CommandCondition.HIDE;
            return world.getPlayerState(player) instanceof ParkourState.Finished
                    ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }
}
