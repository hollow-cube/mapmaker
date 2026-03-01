package net.hollowcube.mapmaker.editor.hdb.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.hdb.HeadDatabaseClient;
import net.hollowcube.mapmaker.editor.hdb.gui.HdbBrowserPanel;
import net.hollowcube.mapmaker.panels.Panel;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HdbSearchCommand extends CommandDsl {
    private final Argument<String> queryArg = Argument.GreedyString("query")
        .defaultValue("").description("The head to search for");

    private final HeadDatabaseClient hdb;

    public HdbSearchCommand(@NotNull HeadDatabaseClient hdb) {
        super("search");
        this.hdb = hdb;

        addSyntax(playerOnly(this::handleSearch));
        addSyntax(playerOnly(this::handleSearch), queryArg);
    }

    private void handleSearch(@NotNull Player player, @NotNull CommandContext context) {
        Panel.open(player, new HdbBrowserPanel(hdb, context.get(queryArg)));
    }

}
