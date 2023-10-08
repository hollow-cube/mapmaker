package net.hollowcube.command.example.map;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.example.map.arg.ExtraArguments;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapListCommand extends Command {

    private final Argument<String> targetArg;

    public MapListCommand() {
        super("list");

        targetArg = ExtraArguments.PlayerIdWithCompletion("target");
        var condition = (CommandCondition) (sender, unused) -> {
            boolean result = true; //todo
            return result ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };

        setDefaultExecutor(playerOnly(this::executeSelf));
        addSyntax(condition, playerOnly(this::executeOther), targetArg);
    }

    private void executeSelf(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("list self");
    }

    private void executeOther(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("list other");
    }

}
