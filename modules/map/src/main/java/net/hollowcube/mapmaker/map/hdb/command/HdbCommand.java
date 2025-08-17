package net.hollowcube.mapmaker.map.hdb.command;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.hdb.gui.HdbBrowserView;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class HdbCommand extends CommandDsl {

    private final Controller guiController;

    public HdbCommand(@NotNull HeadDatabase hdb, @NotNull Controller guiController) {
        super("headdb", "hdb");
        this.guiController = guiController;

        setCondition(mapFilter(false, true, false));

        addSubcommand(new HdbSearchCommand(hdb, guiController));
        addSubcommand(new HdbGiveCommand(hdb));
        addSubcommand(new HdbBase64Command());

        addSyntax(playerOnly(this::handleOpenMainGui));
    }

    private void handleOpenMainGui(@NotNull Player player, @NotNull CommandContext context) {
        this.guiController.show(player, HdbBrowserView::new);
    }

}
