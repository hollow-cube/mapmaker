package net.hollowcube.mapmaker.command.store;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StoreCommand extends CommandDsl {
    private final PlayerService playerService;
    private final PermManager permManager;

    public StoreCommand(@NotNull PlayerService playerService, @NotNull PermManager permManager) {
        super("store", "buy");
        this.playerService = playerService;
        this.permManager = permManager;

        category = CommandCategories.GLOBAL;
        description = "Opens our in-game store";

        addSyntax(playerOnly(this::handleOpenStore));
    }

    private void handleOpenStore(@NotNull Player player, @NotNull CommandContext context) {
        Panel.open(player, new StoreView(playerService, permManager));
    }
}
