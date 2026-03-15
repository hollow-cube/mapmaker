package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.VisibilityRule;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.Locale;

public class HideCommand extends CommandDsl {
    private final PlayerService playerService;

    public HideCommand(PlayerService playerService) {
        super("hide");
        this.playerService = playerService;

        category = CommandCategories.UTILITY;
        description = "Hide or show nearby players";

        addSyntax(playerOnly(this::handleHidePlayers));
    }

    private void handleHidePlayers(Player player, CommandContext context) {
        var playerData = PlayerData.fromPlayer(player);
        var current = playerData.getSetting(PlayerSettings.NEARBY_PLAYER_VISIBILITY);
        var newValue = current == VisibilityRule.GHOST ? VisibilityRule.HIDDEN : VisibilityRule.GHOST;
        playerData.setSetting(PlayerSettings.NEARBY_PLAYER_VISIBILITY, newValue);
        playerData.writeUpdatesUpstream(playerService);
        player.sendMessage(Component.translatable("command.hide." + newValue.name().toLowerCase(Locale.ROOT)));
    }

}
