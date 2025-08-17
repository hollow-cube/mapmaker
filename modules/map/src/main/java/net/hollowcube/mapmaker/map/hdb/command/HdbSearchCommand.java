package net.hollowcube.mapmaker.map.hdb.command;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.hdb.gui.HdbSearchView;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HdbSearchCommand extends CommandDsl {
    private final Argument<String> queryArg = Argument.GreedyString("query")
            .defaultValue("").description("The head to search for");

    private final HeadDatabase hdb;
    private final Controller guiController;

    public HdbSearchCommand(@NotNull HeadDatabase hdb, @NotNull Controller guiController) {
        super("search");
        this.hdb = hdb;
        this.guiController = guiController;

        addSyntax(playerOnly(this::handleSearch));
        addSyntax(playerOnly(this::handleSearch), queryArg);
    }

    private void handleSearch(@NotNull Player player, @NotNull CommandContext context) {
        guiController.show(player, c -> new HdbSearchView(c, context.get(queryArg)));
    }

}
