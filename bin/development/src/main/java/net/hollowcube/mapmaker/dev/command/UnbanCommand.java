package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.mapmaker.storage.BanStorage;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class UnbanCommand extends Command {

    private final BanStorage bans;
    private final Argument<String> unbanTarget = ArgumentType.String("prisoner");
    private final Logger logger = LoggerFactory.getLogger(UnbanCommand.class);

    public UnbanCommand(BanStorage bans) {
        super("unban", "pardon");
        this.bans = bans;

        unbanTarget.setSuggestionCallback((sender, context, suggestion) -> {
            try {
                for (String s : bans.getUnbannedUsernames().get()) {
                    suggestion.addEntry(new SuggestionEntry(s));
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.warn("Failed to retrieve username list from storage!");
                logger.warn(e.getMessage());
            }
        });

        addSyntax(this::unbanPlayers, unbanTarget);
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /unban <player>"));
    }

    private void unbanPlayers(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if(context.has(unbanTarget)) {
            String username = context.get(unbanTarget);
            bans.unbanPlayer(username);
        }
    }
}
