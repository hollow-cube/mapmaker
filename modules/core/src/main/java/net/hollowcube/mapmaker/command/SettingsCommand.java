package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.gui.settings.PlayerSettingsScreen;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SettingsCommand extends CommandDsl {
    public SettingsCommand() {
        super("settings");

        this.description = "Lists all server settings that you can modify";
        this.category = CommandCategories.GLOBAL;

        addSyntax(playerOnly(this::invoke));
    }

    private void invoke(@NotNull Player player, @NotNull CommandContext context) {
        PlayerSettingsScreen.openSettingsDialog(player);
    }
}
