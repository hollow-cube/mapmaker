package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.hollowcube.mapmaker.map.MapVariant;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

public class TestCommand extends CommandDsl {

    public TestCommand() {
        super("test", "build");

        description = "Pause building and enter a playing state to test the gameplay of your map";

        setCondition(this::isInBuildOrTestMap);
        addSyntax(playerOnly(this::enterBuildMode));
    }

    private void enterBuildMode(Player player, CommandContext context) {
        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return; // Sanity

        if (world.map().settings().getVariant() == MapVariant.BUILDING) {
            player.sendMessage(Component.translatable("command.test.gameplay_only"));
            return;
        }

        var nextState = switch (world.getPlayerState(player)) {
            case EditorState.Building(var saveState) -> new EditorState.Testing(saveState);
            case EditorState.Testing(var saveState) -> new EditorState.Building(saveState);
            case null -> null;
        };
        if (nextState != null) world.changePlayerState(player, nextState);
    }

    private int isInBuildOrTestMap(CommandSender sender, CommandContext unused) {
        if (!(sender instanceof Player player))
            return CommandCondition.HIDE;
        return EditorMapWorld.forPlayer(player) != null
                ? CommandCondition.ALLOW : CommandCondition.HIDE;
    }
}
