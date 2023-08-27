package net.hollowcube.map.command;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HubCommand extends BaseMapCommand {
    private static final Logger logger = LoggerFactory.getLogger(HubCommand.class);

    private final MapToHubBridge bridge;

    public HubCommand(@NotNull MapToHubBridge bridge) {
        super("hub", "leave", "l", "lobby");
        this.bridge = bridge;

        setDefaultExecutor(this::returnToHub);
    }

    private void returnToHub(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            return;
        }

        //todo this code is duplicated hella
        try {
            sender.sendMessage("Returning to hub");

            var world = MapWorld.forPlayerOptional(player);
            if (world instanceof InternalMapWorld internalWorld) {
                internalWorld.removePlayer(player);
            }
            bridge.sendPlayerToHub(player);
        } catch (Exception e) {
            logger.error("failed to send player {} to hub: {}", player.getUuid(), e.getMessage());
            LanguageProviderV2.translateMulti("command.generic.unknown_error", List.of())
                    .forEach(player::sendMessage);
        }

//        Scoreboards.showPlayerLobbyScoreboard(player);
    }
}
