package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CommandRewriter {

    private final CommandManager hubCommandManager;
    private final CommandManager mapCommandManager;

    public CommandRewriter(@NotNull CommandManager hubCommandManager, @NotNull CommandManager mapCommandManager) {
        this.hubCommandManager = hubCommandManager;
        this.mapCommandManager = mapCommandManager;
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
            var suggestion = getSuggestion(player, text);
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
        });
    }

    private @NotNull Suggestion getSuggestion(Player commandSender, String text) {
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
        return commandManager.suggestions(commandSender, text);
    }

    private @NotNull CommandManager getCommandManagerForPlayer(@NotNull Player player) {
        return MapWorld.forPlayerOptional(player) != null ? mapCommandManager : hubCommandManager;
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
