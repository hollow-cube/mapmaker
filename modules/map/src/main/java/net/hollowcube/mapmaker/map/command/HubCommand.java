package net.hollowcube.mapmaker.map.command;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HubCommand extends CommandDsl {
    private static final Logger logger = LoggerFactory.getLogger(HubCommand.class);

    private final ServerBridge bridge;

    @Inject
    public HubCommand(@NotNull ServerBridge bridge) {
        super("hub", "leave", "l", "lobby");
        this.bridge = bridge;

        category = CommandCategories.GLOBAL;
        description = "Teleports you to the lobby server";

        addSyntax(playerOnly(this::returnToHub));
    }

    private void returnToHub(@NotNull Player player, @NotNull CommandContext context) {
        try {
            var world = MapWorld.forPlayerOptional(player);
            if (world != null) world.removePlayer(player);
            bridge.joinHub(player);
        } catch (Exception e) {
            logger.error("failed to send player {} to hub: {}", player.getUuid(), e.getMessage());
            LanguageProviderV2.translateMulti("command.generic.unknown_error", List.of())
                    .forEach(player::sendMessage);
        }
    }

}
