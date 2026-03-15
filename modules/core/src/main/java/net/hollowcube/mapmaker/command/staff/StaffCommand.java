package net.hollowcube.mapmaker.command.staff;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.perm;

public class StaffCommand extends CommandDsl {
    private final PlayerService playerService;

    public StaffCommand(PlayerService playerService) {
        super("staff");
        this.playerService = playerService;

        category = CommandCategories.STAFF;
        description = "Toggles staff mode on or off (hides staff-related commands/messages/etc)";

        setCondition(perm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::handleToggleStaffMode));
    }

    private void handleToggleStaffMode(Player player, CommandContext context) {
        var playerData = PlayerData.fromPlayer(player);

        var nextStaffMode = !playerData.getSetting(PlayerSettings.STAFF_MODE);
        playerData.setSetting(PlayerSettings.STAFF_MODE, nextStaffMode);

        // If in staff chat and disabling staff mode, switch to global
        if (!nextStaffMode && ClientChatMessageData.CHANNEL_STAFF.equals(playerData.getSetting(PlayerSettings.CHAT_CHANNEL))) {
            playerData.setSetting(PlayerSettings.CHAT_CHANNEL, ClientChatMessageData.CHANNEL_GLOBAL);
        }

        player.refreshCommands();

        try {
            player.sendMessage(nextStaffMode ? "staff mode enabled" : "staff mode disabled");

            playerData.setSetting(PlayerSettings.IS_VANISHED, true);
            playerData.writeUpdatesUpstream(playerService);
        } catch (Exception e) {
            player.sendMessage("something went wrong");
            ExceptionReporter.reportException(e, player);
        }
    }
}
