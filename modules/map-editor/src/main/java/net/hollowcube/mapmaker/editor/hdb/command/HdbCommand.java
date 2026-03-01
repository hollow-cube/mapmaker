package net.hollowcube.mapmaker.editor.hdb.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.hdb.HeadDatabaseClient;
import net.hollowcube.mapmaker.editor.hdb.gui.HdbBrowserPanel;
import net.hollowcube.mapmaker.panels.Panel;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class HdbCommand extends CommandDsl {

    private final HeadDatabaseClient hdb;

    public HdbCommand(@NotNull HeadDatabaseClient hdb) {
        super("headdb", "hdb");
        this.hdb = hdb;

        setCondition(builderOnly());

        addSubcommand(new HdbSearchCommand(hdb));
        addSubcommand(new HdbGiveCommand(hdb));
        addSubcommand(new HdbBase64Command());

        addSyntax(playerOnly(this::handleOpenMainGui));
    }

    private void handleOpenMainGui(@NotNull Player player, @NotNull CommandContext context) {
        Panel.open(player, new HdbBrowserPanel(hdb));
    }

}
