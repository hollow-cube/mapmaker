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

public class VanishCommand extends CommandDsl {
    private final Argument<String> silentArg = Argument.Literal("silent")
            .description("Do not send a disconnect message");

    private final SessionManager sessionManager;
    private final PlayerService playerService;

    public VanishCommand(@NotNull SessionManager sessionManager, @NotNull PlayerService playerService, @NotNull PermManager permManager) {
        super("vanish", "v");
        this.sessionManager = sessionManager;
        this.playerService = playerService;

        category = CommandCategories.STAFF;
        description = "Vanish from other players. Does nothing if you are already vanished";

        setCondition(permManager.createPlatformCondition2(PlatformPerm.VANISH));
        addSyntax(playerOnly(this::handleVanish), silentArg);
        addSyntax(playerOnly(this::handleVanish));
    }

    private void handleVanish(@NotNull Player player, @NotNull CommandContext context) {
        boolean isSilent = context.has(silentArg);

        var playerData = PlayerData.fromPlayer(player);
        if (sessionManager.isHidden(playerData.id())) {
            player.sendMessage("you are already hidden");
            return;
        }

        try {
            sessionManager.updateState(playerData.id(), SessionStateUpdateRequest.hidden(true, isSilent));
            player.sendMessage("you are now hidden");

            playerData.setSetting(PlayerSettings.IS_VANISHED, true);
            playerData.writeUpdatesUpstream(playerService);
        } catch (Exception e) {
            player.sendMessage("an error occurred while hiding you");
            ExceptionReporter.reportException(e, player);
        }
    }

}
