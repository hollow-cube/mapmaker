package net.hollowcube.terraform.demo;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.arg.SuggestionResult;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CommandRewriter {

    public static void init(@NotNull CommandManager commandManager) {
        var rewriter = new CommandRewriter(commandManager);

        var connectionManager = MinecraftServer.getConnectionManager();
        connectionManager.setPlayerProvider(rewriter::createPlayer);

        var packetListenerManager = MinecraftServer.getPacketListenerManager();
        packetListenerManager.setListener(ClientCommandChatPacket.class, rewriter::execCommand);
        packetListenerManager.setListener(ClientTabCompletePacket.class, rewriter::tabCommand);
    }

    private final CommandManager commandManager;

    public CommandRewriter(@NotNull CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public @NotNull Player createPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection connection) {
        return new RewritingPlayer(uuid, username, connection);
    }

    public void execCommand(@NotNull ClientCommandChatPacket packet, @NotNull Player player) {
        final String command = packet.message();
        if (Messenger.canReceiveCommand(player)) {
            var commandManager = getCommandManagerForPlayer(player);
            commandManager.execute(player, command);
        } else {
            Messenger.sendRejectionMessage(player);
        }
    }

    public void tabCommand(@NotNull ClientTabCompletePacket packet, @NotNull Player player) {
        Thread.startVirtualThread(() -> {
            final String text = packet.text();
            final SuggestionResult.Success suggestion = getSuggestion(player, text);
            if (suggestion != null) {
                player.sendPacket(new TabCompletePacket(
                        packet.transactionId(),
                        suggestion.start() + 1,
                        suggestion.length(),
                        suggestion.suggestions().stream()
                                .map(suggestionEntry -> new TabCompletePacket.Match(suggestionEntry.replacement(), suggestionEntry.tooltip()))
                                .toList())
                );
            }
        });
    }

    private @Nullable SuggestionResult.Success getSuggestion(Player commandSender, String text) {
        if (text.startsWith("/")) {
            text = text.substring(1);
        }
//        if (text.endsWith(" ")) {
//            // Append a placeholder char if the command ends with a space allowing the parser to find suggestion
//            // for the next arg without typing the first char of it, this is probably the most hacky solution, but hey
//            // it works as intended :)
//            text = text + '\00';
//        }

        var commandManager = getCommandManagerForPlayer(commandSender);
        if (commandManager.suggestions(commandSender, text) instanceof SuggestionResult.Success success)
            return success;
        return null;
    }

    private @NotNull CommandManager getCommandManagerForPlayer(@NotNull Player player) {
        return commandManager;
    }

    private class RewritingPlayer extends Player {

        public RewritingPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection playerConnection) {
            super(uuid, username, playerConnection);
        }

        @Override
        public void refreshCommands() {
            sendPacket(getCommandManagerForPlayer(this).commandPacket(this));
        }
    }
}
