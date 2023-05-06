package net.hollowcube.map.command;

import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.ui.Scoreboards;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        try {
            sender.sendMessage("Returning to hub");
            bridge.sendPlayerToHub(player);
        } catch (Exception e) {
            logger.error("failed to send player {} to hub: {}", player.getUuid(), e.getMessage());
            LanguageProvider.createMultiTranslatable("command.generic.unknown_error")
                    .forEach(player::sendMessage);
        }

        Scoreboards.showPlayerLobbyScoreboard(player);
    }
}
