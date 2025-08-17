package net.hollowcube.mapmaker.command.staff;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnvanishCommand extends CommandDsl {
    private final Argument<String> silentArg = Argument.Literal("silent")
            .description("Do not send a join message");

    private final SessionManager sessionManager;
    private final PlayerService playerService;

    public UnvanishCommand(@NotNull SessionManager sessionManager, @NotNull PlayerService playerService, @NotNull PermManager permManager) {
        super("unvanish");
        this.sessionManager = sessionManager;
        this.playerService = playerService;

        category = CommandCategories.STAFF;
        description = "Show yourself to other players. Does nothing if you are not vanished";

        setCondition(permManager.createPlatformCondition2(PlatformPerm.VANISH));
        addSyntax(playerOnly(this::handleVanish), silentArg);
        addSyntax(playerOnly(this::handleVanish));
    }

    private void handleVanish(@NotNull Player player, @NotNull CommandContext context) {
        boolean isSilent = context.has(silentArg);

        var playerData = PlayerData.fromPlayer(player);
        if (!sessionManager.isHidden(playerData.id())) {
            player.sendMessage("you are already visible");
            return;
        }

        try {
            sessionManager.updateState(playerData.id(), SessionStateUpdateRequest.hidden(false, isSilent));
            player.sendMessage("you are now visible");

            playerData.setSetting(PlayerSettings.IS_VANISHED, false);
            playerData.writeUpdatesUpstream(playerService);
        } catch (Exception e) {
            player.sendMessage("an error occurred while showing you");
            ExceptionReporter.reportException(e, player);
        }
    }

}
