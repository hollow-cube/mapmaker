package net.hollowcube.mapmaker.command.staff;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StaffCommand extends CommandDsl {
    public static final CommandCondition IN_STAFF_MODE = (sender, _) ->
        sender instanceof Player p && PlayerData.fromPlayer(p).getSetting(PlayerSettings.STAFF_MODE)
            ? CommandCondition.ALLOW : CommandCondition.HIDE;

    private final PlayerService playerService;

    public StaffCommand(@NotNull PlayerService playerService, @NotNull PermManager permManager) {
        super("staff");
        this.playerService = playerService;

        category = CommandCategories.STAFF;
        description = "Toggles staff mode on or off (hides staff-related commands/messages/etc)";

        setCondition(permManager.createPlatformCondition2(PlatformPerm.VANISH));
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
