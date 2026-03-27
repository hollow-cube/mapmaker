package net.hollowcube.mapmaker.runtime.building.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.runtime.building.BuildingMapWorld;
import net.minestom.server.entity.Player;

public final class BuildingConditions {

    public static CommandCondition buildingWorld() {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            return BuildingMapWorld.forPlayer(player) != null ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }
}
