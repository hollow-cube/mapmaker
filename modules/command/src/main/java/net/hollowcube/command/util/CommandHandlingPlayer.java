package net.hollowcube.command.util;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandResult;
import net.hollowcube.command.arg.Argument;
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
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Implements a player which handles commands. Can be overridden for additional functionality.
 *
 * <p>It is valid to override BLAH BLAH BLAH</p>
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class CommandHandlingPlayer extends Player {
    private static boolean initialized = false;

    public static @NotNull PlayerProvider createDefaultProvider(@NotNull CommandManager commandManager) {
        return (uuid, username, connection) -> new CommandHandlingPlayer(uuid, username, connection) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                return commandManager;
            }
        };
    }

    public CommandHandlingPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection playerConnection) {
        super(uuid, username, playerConnection);

        if (!initialized) {
            initialized = true;

            var packetListenerManager = MinecraftServer.getPacketListenerManager();
            packetListenerManager.setPlayListener(ClientCommandChatPacket.class, CommandHandlingPlayer::execCommand);
            packetListenerManager.setPlayListener(ClientTabCompletePacket.class, CommandHandlingPlayer::tabCommand);
        }
    }

    @Override
    public void refreshCommands() {
        sendPacket(getCommandManager().createCommandPacket(this));
    }

    public abstract @NotNull CommandManager getCommandManager();

    private static void execCommand(@NotNull ClientCommandChatPacket packet, @NotNull Player player) {
        final String command = packet.message();
        if (Messenger.canReceiveCommand(player)) {
            Thread.startVirtualThread(() -> {
                try {
                    if (player instanceof CommandHandlingPlayer pl) {
                        var result = pl.getCommandManager().execute(player, command);
                        switch (result) {
                            case CommandResult.Success() -> {
                            }
                            case CommandResult.SyntaxError(int start, Argument<?> arg) -> {
                                player.sendMessage(Component.text()
                                        .append(Component.text("Syntax error:", NamedTextColor.RED)).appendNewline()
                                        .append(Component.text(command.substring(0, start), NamedTextColor.GRAY))
                                        .append(Component.text(command.substring(start), NamedTextColor.RED, TextDecoration.UNDERLINED))
                                        .append(Component.text("<--[HERE]", NamedTextColor.RED))
                                        .build());
                            }
                            case CommandResult.ExecutionError(Throwable e) -> {
                                player.sendMessage(Component.translatable("generic.unknown_error"));
                                MinecraftServer.getExceptionManager().handleException(new RuntimeException(
                                        "An unhandled exception occurred while executing the command '" + command + "'", e));
                            }
                            case CommandResult.Denied() -> {
                                player.sendMessage("permission error");
                            }
                        }
                    } else throw new RuntimeException("Player is not a CommandHandlingPlayer");
                } catch (Exception e) {
                    MinecraftServer.getExceptionManager().handleException(e);
                }
            });
        } else {
            Messenger.sendRejectionMessage(player);
        }
    }

    private static void tabCommand(@NotNull ClientTabCompletePacket packet, @NotNull Player player) {
        if (!Messenger.canReceiveCommand(player)) return;

        CommandManager commandManager;
        if (player instanceof CommandHandlingPlayer pl) {
            commandManager = pl.getCommandManager();
        } else throw new RuntimeException("Player is not a CommandHandlingPlayer");

        // Collect the suggestions
        Thread.startVirtualThread(() -> {
            try {
                var text = packet.text();
                if (text.startsWith("/"))
                    text = text.substring(1);

                var suggestion = commandManager.suggest(player, text);
                if (!suggestion.isEmpty()) {
                    player.sendPacket(new TabCompletePacket(
                            packet.transactionId(),
                            suggestion.getStart() + 1,
                            suggestion.getLength(),
                            suggestion.getEntries().stream()
                                    .map(suggestionEntry -> new TabCompletePacket.Match(suggestionEntry.replacement(), suggestionEntry.tooltip()))
                                    .toList())
                    );
                }
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        });
    }

}
