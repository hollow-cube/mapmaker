package net.hollowcube.command.util;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandResult;
import net.hollowcube.common.extensions.ExtendedPlayer;
import net.hollowcube.posthog.PostHog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.PlayerProvider;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a player which handles commands. Can be overridden for additional functionality.
 *
 * <p>It is valid to override BLAH BLAH BLAH</p>
 */
public abstract class CommandHandlingPlayer extends ExtendedPlayer {

    private static final Logger log = LoggerFactory.getLogger(CommandHandlingPlayer.class);

    private static final Tag<Long> LAST_COMMAND = Tag.Long("last_command").defaultValue(0L);
    private static final long COMMAND_COOLDOWN = 350; // milliseconds

    static {
        var packetListenerManager = MinecraftServer.getPacketListenerManager();
        packetListenerManager.setPlayListener(ClientCommandChatPacket.class, CommandHandlingPlayer::execCommand);
        packetListenerManager.setPlayListener(ClientTabCompletePacket.class, CommandHandlingPlayer::tabCommand);
    }

    public CommandHandlingPlayer(@NotNull PlayerConnection connection, @NotNull GameProfile gameProfile) {
        super(connection, gameProfile);
    }

    public static @NotNull PlayerProvider createDefaultProvider(@NotNull CommandManager commandManager) {
        return (connection, gameProfile) -> new CommandHandlingPlayer(connection, gameProfile) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                return commandManager;
            }
        };
    }

    private static void execCommand(@NotNull ClientCommandChatPacket packet, @NotNull Player player) {
        final String command = packet.message();

        if (!Messenger.canReceiveCommand(player)) {
            Messenger.sendRejectionMessage(player);
            return;
        }

        // Rate limit player commands
        long now = System.currentTimeMillis();
        if (now - player.getTag(LAST_COMMAND) < COMMAND_COOLDOWN) {
            player.sendMessage(Component.translatable("command.rate_limited"));
            return;
        }
        player.setTag(LAST_COMMAND, now);

        Thread.startVirtualThread(() -> {
            try {
                var manager = CommandHandlingPlayer.asHandled(player).getCommandManager();

                switch (manager.execute(player, command)) {
                    case CommandResult.Success ignored -> {
                    }
                    case CommandResult.Denied ignored ->
                        player.sendMessage(Component.translatable("command.not_found"));
                    case CommandResult.NotFound ignored ->
                        player.sendMessage(Component.translatable("command.not_found"));
                    case CommandResult.SyntaxError result -> {
                        var errorMessage = result.message();
                        var builder = Component.text()
                            .append(Component.text("Syntax error:", NamedTextColor.RED))
                            .appendNewline()
                            .append(Component.text(command.substring(0, result.start()), NamedTextColor.GRAY))
                            .append(Component.text(command.substring(result.start()), NamedTextColor.RED,
                                                   TextDecoration.UNDERLINED))
                            .append(Component.text("<--[HERE] ", NamedTextColor.RED));
                        if (errorMessage != null) {
                            builder.append(Component.text(errorMessage, NamedTextColor.RED));
                        }
                        player.sendMessage(builder.build());
                    }
                    case CommandResult.ExecutionError result -> {
                        player.sendMessage(Component.translatable("generic.unknown_error"));
                        var syntheticException = new RuntimeException(
                            "An unhandled exception occurred while executing the command '" + command + "'",
                            result.cause());
                        PostHog.captureException(syntheticException, player.getUuid().toString());
                        log.error("command eval failure", syntheticException);
                    }
                }
            } catch (Exception e) {
                PostHog.captureException(e, player.getUuid().toString());
                log.error("command failure", e);
            }
        });
    }

    private static void tabCommand(@NotNull ClientTabCompletePacket packet, @NotNull Player player) {
        if (!Messenger.canReceiveCommand(player)) return;

        var manager = CommandHandlingPlayer.asHandled(player).getCommandManager();

        // Collect the suggestions
        Thread.startVirtualThread(() -> {
            try {
                var text = packet.text();
                if (text.startsWith("/"))
                    text = text.substring(1);

                var suggestion = manager.suggest(player, text);
                if (!suggestion.isEmpty()) {
                    player.sendPacket(new TabCompletePacket(
                        packet.transactionId(),
                        suggestion.getStart() + 1,
                        suggestion.getLength(),
                        suggestion.getEntries().stream()
                            .map(entry -> new TabCompletePacket.Match(
                                entry.replacement(),
                                entry.tooltip()
                            ))
                            .toList())
                    );
                }
            } catch (Exception e) {
                PostHog.captureException(e, player.getUuid().toString());
                log.error("command suggestion failure on: '" + packet.text() + "'", e);
            }
        });
    }

    private static CommandHandlingPlayer asHandled(Player player) {
        if (player instanceof CommandHandlingPlayer handlingPlayer) {
            return handlingPlayer;
        }
        throw new IllegalArgumentException("Player is not a CommandHandlingPlayer");
    }

    @Override
    public void refreshCommands() {
        sendPacket(getCommandManager().createCommandPacket(this));
    }

    public abstract @NotNull CommandManager getCommandManager();

}
