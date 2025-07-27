package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.minestom.server.entity.Player;

public final class EditorConditions {

    public static CommandCondition editorWorld() {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            return EditorMapWorld.forPlayer(player) != null ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }

    public static CommandCondition builderOnly() {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            var world = EditorMapWorld.forPlayer(player);
            if (world == null) return CommandCondition.HIDE;
            return world.getPlayerState(player) instanceof EditorState.Building
                    ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }

    public static CommandCondition testerOnly() {
        return (sender, _) -> {
            if (!(sender instanceof Player player)) return CommandCondition.HIDE;
            var world = EditorMapWorld.forPlayer(player);
            if (world == null) return CommandCondition.HIDE;
            return world.getPlayerState(player) instanceof EditorState.Testing
                    ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }

}
