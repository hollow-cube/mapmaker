package net.hollowcube.map.command.build;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BuildCommand extends Command {

    public BuildCommand() {
        super("build");

        setCondition(this::isInTestingMap);
        addSyntax(playerOnly(this::enterBuildMode));
    }

    private void enterBuildMode(@NotNull Player player, @NotNull CommandContext context) {
        var map = MapWorld.forPlayerOptional(player);
        if (map instanceof TestingMapWorld testingWorld) {
            testingWorld.exitTestMode(player);
        } else throw new IllegalStateException("unreachable");
    }

    private int isInTestingMap(@NotNull CommandSender sender, @NotNull CommandContext unused) {
        // Stupid amount of checks to verify they're actually in a world otherwise nullptr exception
        // Probably better way to structure instantiation of this command so it doesn't have this issue
        if (!(sender instanceof Player player)) {
            return CommandCondition.HIDE;
        }

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return CommandCondition.HIDE;

        return (world.flags() & MapWorld.FLAG_TESTING) != 0 ? CommandCondition.ALLOW : CommandCondition.HIDE;
    }
}
