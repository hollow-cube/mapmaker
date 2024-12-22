package net.hollowcube.mapmaker.command;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.gui.store.CosmeticView;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CosmeticsCommand extends CommandDsl {

    private final Controller controller;

    public CosmeticsCommand(@NotNull Controller controller) {
        super("cosmetics", "cosmetic");

        this.controller = controller;

        addSyntax(playerOnly(this::handleOpenCosmeticMenu));
    }

    private void handleOpenCosmeticMenu(@NotNull Player player, @NotNull CommandContext context) {
        controller.show(player, CosmeticView::new);
    }

}
