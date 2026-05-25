package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.minestom.server.entity.Player;

public final class MapCommandCondition {

    public static CommandCondition mapSetting(MapSetting<Boolean> setting) {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            var world = MapWorld.forPlayer(player);
            if (world == null) return CommandCondition.HIDE;
            return world.map().getSetting(setting) ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }
}
