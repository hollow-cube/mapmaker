package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.interaction.Command;
import net.hollowcube.mapmaker.api.interaction.Interaction;
import net.hollowcube.mapmaker.api.interaction.InteractionClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.perm;
import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class RemoteCommand extends CommandDsl {
    private final InteractionClient interactions;
    private final Command decl;

    private final Argument<?>[] args;

    public RemoteCommand(ApiClient api, PlayerService playerService, Command decl) {
        super(decl.name());
        this.interactions = api.interactions;
        this.decl = decl;

        if (decl.description() != null)
            this.description = decl.description();
        if (decl.permissions() != 0) {
            var isStaff = (decl.permissions() & Permission.GENERIC_STAFF) != 0;
            setCondition(isStaff ? staffPerm(decl.permissions()) : perm(decl.permissions()));
        }

        boolean hadOptional = false;
        this.args = new Argument<?>[decl.arguments().size()];
        for (int i = 0; i < decl.arguments().size(); i++) {
            var remoteArg = decl.arguments().get(i);

            if (hadOptional && !remoteArg.optional())
                throw new IllegalArgumentException("Optional arguments must be at the end of the argument list");
            if (remoteArg.optional()) {
                hadOptional = true;

                // If this arg is optional, register the prevous syntax
                addSyntax(playerOnly(this::execute), Arrays.copyOf(this.args, i));
            }

            this.args[i] = switch (remoteArg.type()) {
                case WORD -> Argument.Word(remoteArg.name());
                case STRING -> Argument.GreedyString(remoteArg.name());
                case CHOICE -> Argument.Word(remoteArg.name()).with(Objects.requireNonNull(remoteArg.choices()));
                case PLAYER -> CoreArgument.AnyPlayerId(remoteArg.name(), playerService);
            };
        }

        addSyntax(playerOnly(this::execute), this.args);
    }

    private void execute(Player player, CommandContext context) {
        var interactionArgs = new ArrayList<Interaction.CommandArgument>();
        for (int i = 0; i < this.args.length; i++) {
            var argDecl = decl.arguments().get(i);
            var arg = this.args[i];
            if (!context.has(arg)) break;

            var argValue = context.get(arg);
            interactionArgs.add(new Interaction.CommandArgument(argDecl.name(), argDecl.type(), argValue));
        }

        var playerId = PlayerData.fromPlayer(player).id();
        var interaction = new Interaction(decl.name(), Interaction.Type.COMMAND, playerId, new Interaction.CommandData(interactionArgs));
        var response = interactions.execute(interaction);
        player.sendMessage(response.resolveMessage());
    }

}
