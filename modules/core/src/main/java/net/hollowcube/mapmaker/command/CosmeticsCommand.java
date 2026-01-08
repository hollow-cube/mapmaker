package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.gui.store.CosmeticPanel;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CosmeticsCommand extends CommandDsl {

    private final PlayerService players;

    public CosmeticsCommand(@NotNull PlayerService players) {
        super("cosmetics", "cosmetic");

        this.players = players;

        addSyntax(playerOnly(this::handleOpenCosmeticMenu));
    }

    private void handleOpenCosmeticMenu(@NotNull Player player, @NotNull CommandContext context) {
        Panel.open(player, new CosmeticPanel(this.players));
    }

}
