package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.gui.BuilderMenuPanel;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.panels.Panel;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class BuilderMenuCommand extends CommandDsl {

    public BuilderMenuCommand() {
        super("buildermenu", "bm");

        description = "Open the builder menu with a command instead of an item";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::openBuilderMenu));
    }

    private void openBuilderMenu(Player player, CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (world == null) return; // Sanity
        Panel.open(player, new BuilderMenuPanel(world.server().bridge()));
    }

}
