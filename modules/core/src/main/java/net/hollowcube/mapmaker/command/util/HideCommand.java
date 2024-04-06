package net.hollowcube.mapmaker.command.util;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.VisibilityRule;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class HideCommand extends CommandDsl {
    private final PlayerService playerService;

    @Inject
    public HideCommand(@NotNull PlayerService playerService) {
        super("hide");
        this.playerService = playerService;

        category = CommandCategories.UTILITY;
        description = "Hide or show nearby players";

        addSyntax(playerOnly(this::handleHidePlayers));
    }

    private void handleHidePlayers(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = PlayerDataV2.fromPlayer(player);
        var current = playerData.getSetting(PlayerSettings.NEARBY_PLAYER_VISIBILITY);
        var newValue = current == VisibilityRule.GHOST ? VisibilityRule.HIDDEN : VisibilityRule.GHOST;
        playerData.setSetting(PlayerSettings.NEARBY_PLAYER_VISIBILITY, newValue);
        playerData.writeUpdatesUpstream(playerService);
        player.sendMessage(Component.translatable("command.hide." + newValue.name().toLowerCase(Locale.ROOT)));
    }

}
