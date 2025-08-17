package net.hollowcube.mapmaker.runtime.parkour.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.runtime.parkour.hud.ResetHeightDisplay;
import net.minestom.server.entity.Player;

public class ShowHeightCommand extends CommandDsl {

    public ShowHeightCommand() {
        super("showheight");

        category = CommandCategories.MAP;
        description = "Toggles a reset height display for the map";

        setCondition(ParkourConditions.anyPlayingOnly());
        addSyntax(playerOnly(this::execute));
    }

    private void execute(Player player, CommandContext context) {
        ResetHeightDisplay.toggle(player);
    }

}
