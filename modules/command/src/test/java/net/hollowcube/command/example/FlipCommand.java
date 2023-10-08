package net.hollowcube.command.example;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentAxis;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Should support the following syntaxes:
// /flip
// /flip x
// /flip [string]
// /flip x [string]
public class FlipCommand extends Command {
    public final Argument<ArgumentAxis.@Nullable Result> optAxisArg = Argument.Opt(Argument.Axis("axis"));
    public final Argument<@Nullable String> optWordArg = Argument.Opt(Argument.Word("word")
            .with("clip1", "clip_2"));

    public FlipCommand(@Nullable CommandExecutor executor, @Nullable CommandExecutor wordErrorHandler) {
        super("flip");

        if (wordErrorHandler != null) {
            optWordArg.errorHandler(wordErrorHandler);
        }

        var exec = executor == null ? playerOnly(this::execute) : executor;
        addSyntax(exec, optAxisArg, optWordArg);

        //todo if an invalid input is given to the last arg, it should call an error handler on it.
        // In this case, if you do something like "flip not_a_clipboard" we should call an error handler
        // on the clipboard arg which can reply saying no clipboard.
        // If an error handler is not present, it should just hit the default executor.
    }

    private void showHelp(@NotNull Player player, @NotNull CommandContext context) {

    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var axis = context.get(optAxisArg);
        var word = context.get(optWordArg);
        player.sendMessage("exec axis=" + axis + " word=" + word);
    }

}
