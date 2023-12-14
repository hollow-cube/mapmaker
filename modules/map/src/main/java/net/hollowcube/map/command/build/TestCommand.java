package net.hollowcube.map.command.build;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TestCommand extends Command {

    public TestCommand() {
        super("test");

        setCondition(this::isInBuildOrTestMap);
        addSyntax(playerOnly(this::enterBuildMode));
    }

    private void enterBuildMode(@NotNull Player player, @NotNull CommandContext context) {
        var map = MapWorld.forPlayerOptional(player);
        if (map instanceof TestingMapWorld testingWorld) {
            testingWorld.exitTestMode(player);
        } else if (map.map().settings().getVariant() == MapVariant.BUILDING) {
            player.sendMessage(Component.translatable("command.test.gameplay_only"));
        } else if (map instanceof EditingMapWorld editingWorld) {
            editingWorld.enterTestMode(player);
        } else throw new IllegalStateException("unreachable");
    }

    private int isInBuildOrTestMap(@NotNull CommandSender sender, @NotNull CommandContext unused) {
        // Stupid amount of checks to verify they're actually in a world otherwise nullptr exception
        // Probably better way to structure instantiation of this command so it doesn't have this issue
        if (!(sender instanceof Player player)) {
            return CommandCondition.HIDE;
        }

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return CommandCondition.HIDE;

        return (world.flags() & (MapWorld.FLAG_TESTING | MapWorld.FLAG_EDITING)) != 0 ? CommandCondition.ALLOW : CommandCondition.HIDE;
    }
}
