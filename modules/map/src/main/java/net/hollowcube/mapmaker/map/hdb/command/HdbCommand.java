package net.hollowcube.mapmaker.map.hdb.command;

import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HdbCommand extends CommandDsl {

    private final HeadDatabase hdb;
    private final Controller guiController;

    @Inject
    public HdbCommand(@NotNull HeadDatabase hdb, @NotNull Controller guiController) {
        super("headdb", "hdb");
        this.hdb = hdb;
        this.guiController = guiController;

        addSubcommand(new HdbSearchCommand(hdb, guiController));
        addSubcommand(new HdbGiveCommand(hdb));
        addSubcommand(new HdbBase64Command(hdb));

        addSyntax(playerOnly(this::handleOpenMainGui));
    }

    private void handleOpenMainGui(@NotNull Player player, @NotNull CommandContext context) {
//        this.guiController.show(player, );
    }

}
