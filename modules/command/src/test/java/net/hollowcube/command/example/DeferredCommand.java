package net.hollowcube.command.example;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.arg.Argument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class DeferredCommand extends Command {
    public final Argument<@Nullable String> optWordArg = Argument.Word("word")
            .map((sender, word) -> new Argument.ParseDeferredSuccess<>(() -> {
                try {
                    Thread.sleep(500);
                    return word.toUpperCase(Locale.ROOT);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));

    public DeferredCommand(@Nullable CommandExecutor executor) {
        super("defer");

        addSyntax(executor == null ? playerOnly(this::execute) : executor, optWordArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var word = context.get(optWordArg);
        player.sendMessage("exec word=" + word);
    }

}
