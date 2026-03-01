package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.interaction.RemoteInteraction;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

public class RemoteCommand extends CommandDsl {
    private final PlayerService playerService;
    private final RemoteInteraction decl;

    public RemoteCommand(PlayerService playerService, RemoteInteraction decl) {
        super(decl.name());
        this.playerService = playerService;
        this.decl = decl;
    }

    @Override
    public void build(@NotNull CommandBuilder builder) {
        builder
            .description("remote decl")
        ;

        var cmdArgs = new ArrayList<Argument<?>>();
        for (var arg : decl.arguments()) {
            cmdArgs.add(switch (arg.type()) {
                case WORD -> Argument.Word(arg.name());
                case STRING -> Argument.GreedyString(arg.name());
                case CHOICE -> Argument.Word(arg.name()).with(Objects.requireNonNull(arg.choices()));
                case PLAYER -> CoreArgument.AnyPlayerId(arg.name(), playerService);
                case DYNAMIC -> throw new UnsupportedOperationException("todo");
            });
        }
        builder.executes((p, c) -> {
            p.sendMessage("remote cmd exec: ");
            for (int i = 0; i < cmdArgs.size(); i++) {
                p.sendMessage("arg " + i + ": " + c.getRaw(cmdArgs.get(i)));
            }
        }, cmdArgs.toArray(new Argument<?>[0]));
    }

}
