package net.hollowcube.mapmaker.dev.command.map;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.map.worldold.MapWorldManager;
import net.hollowcube.mapmaker.dev.command.map.world.MapWorldListCommand;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import org.jetbrains.annotations.NotNull;

public class MapWorldCommand extends CommandDsl {
    public MapWorldCommand(@NotNull MapWorldManager mwm, @NotNull PermManager permManager) {
        super("world");

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));

        addSubcommand(new MapWorldListCommand(mwm));
    }
}
