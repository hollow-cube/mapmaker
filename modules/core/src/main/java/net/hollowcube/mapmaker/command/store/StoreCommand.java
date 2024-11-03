package net.hollowcube.mapmaker.command.store;

import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StoreCommand extends CommandDsl {
    private final Controller guiController;

    @Inject
    public StoreCommand(@NotNull Controller guiController) {
        super("store", "buy");
        this.guiController = guiController;

        category = CommandCategories.GLOBAL;
        description = "Opens our in-game store";

        addSyntax(playerOnly(this::handleOpenStore));
    }

    private void handleOpenStore(@NotNull Player player, @NotNull CommandContext context) {
        guiController.show(player, StoreView::new);
    }
}
