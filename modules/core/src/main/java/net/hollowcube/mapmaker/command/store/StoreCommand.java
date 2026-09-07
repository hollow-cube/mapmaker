package net.hollowcube.mapmaker.command.store;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.AccountService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StoreCommand extends CommandDsl {
    private final AccountService accountService;

    public StoreCommand(@NotNull AccountService accountService) {
        super("store", "buy");
        this.accountService = accountService;

        category = CommandCategories.GLOBAL;
        description = "Opens our in-game store";

        addSyntax(playerOnly(this::handleOpenStore));
    }

    private void handleOpenStore(@NotNull Player player, @NotNull CommandContext context) {
        Panel.open(player, new StoreView(accountService));
    }
}
