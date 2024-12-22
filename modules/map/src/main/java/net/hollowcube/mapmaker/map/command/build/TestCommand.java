package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TestCommand extends CommandDsl {

    public TestCommand() {
        super("test");

        description = "Pause building and enter a playing state to test the gameplay of your map";

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

        return world.canEdit(player) || world instanceof TestingMapWorld ? CommandCondition.ALLOW : CommandCondition.HIDE;
    }
}
