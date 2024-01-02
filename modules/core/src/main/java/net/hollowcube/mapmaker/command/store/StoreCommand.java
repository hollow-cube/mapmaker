package net.hollowcube.mapmaker.command.store;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.command.util.CoreCondition.feature;

public class StoreCommand extends Command {
    private final Controller guiController;

    public StoreCommand(@NotNull Controller guiController) {
        super("store", "buy");
        this.guiController = guiController;

        setCondition(feature(CoreFeatureFlags.STORE));

        addSyntax(playerOnly(this::handleOpenStore));
    }

    private void handleOpenStore(@NotNull Player player, @NotNull CommandContext context) {
        guiController.show(player, StoreView::new);
    }
}
