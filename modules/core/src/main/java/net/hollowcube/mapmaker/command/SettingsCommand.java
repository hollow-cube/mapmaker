package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.gui.settings.PlayerSettingsScreen;
import net.minestom.server.entity.Player;

public class SettingsCommand extends CommandDsl {
    public SettingsCommand() {
        super("settings");

        this.description = "Lists all server settings that you can modify";
        this.category = CommandCategories.GLOBAL;

        addSyntax(playerOnly(this::invoke));
    }

    private void invoke(Player player, CommandContext context) {
        PlayerSettingsScreen.openSettingsDialog(player);
    }
}
