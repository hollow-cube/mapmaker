package net.hollowcube.map.command;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HubCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(HubCommand.class);

    private final MapToHubBridge bridge;
    private final PlayerInviteService inviteService;

    public HubCommand(@NotNull MapToHubBridge bridge, PlayerInviteService inviteService) {
        super("hub", "leave", "lobby");
        this.bridge = bridge;
        this.inviteService = inviteService;

        setDefaultExecutor(playerOnly(this::returnToHub));
    }

    private void returnToHub(@NotNull Player player, @NotNull CommandContext context) {
        try {
            //todo this code is duplicated hella
            //player.sendMessage("Returning to hub");

            var world = MapWorld.forPlayerOptional(player);
            if (world instanceof InternalMapWorld internalWorld) {
                internalWorld.removePlayer(player);
            }
            inviteService.invalidateInvitesAndRequests(player);
            bridge.sendPlayerToHub(player);
        } catch (Exception e) {
            logger.error("failed to send player {} to hub: {}", player.getUuid(), e.getMessage());
            LanguageProviderV2.translateMulti("command.generic.unknown_error", List.of())
                    .forEach(player::sendMessage);
        }
    }

}
