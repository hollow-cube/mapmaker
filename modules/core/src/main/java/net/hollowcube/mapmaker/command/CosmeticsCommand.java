package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.gui.store.CosmeticPanel;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;

public class CosmeticsCommand extends CommandDsl {

    private final PlayerService players;

    public CosmeticsCommand(PlayerService players) {
        super("cosmetics", "cosmetic");

        this.players = players;

        addSyntax(playerOnly(this::handleOpenCosmeticMenu));
    }

    private void handleOpenCosmeticMenu(Player player, CommandContext context) {
        Panel.open(player, new CosmeticPanel(this.players));
    }

}
