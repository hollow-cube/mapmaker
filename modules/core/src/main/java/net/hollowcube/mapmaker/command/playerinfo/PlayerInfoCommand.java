package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class PlayerInfoCommand extends CommandDsl {

    public PlayerInfoCommand(
        @NotNull ApiClient api,
        @NotNull SessionManager sessionManager
    ) {
        super("playerinfo");

        category = CommandCategory.HIDDEN;

        setCondition(staffPerm(Permission.GENERIC_STAFF));

        addSubcommand(new SubCommand<>("general", new GeneralInfoType(sessionManager)));
        addSubcommand(new SubCommand<>("channels", new ChannelsInfoType(false)));
        addSubcommand(new SubCommand<>("channel_namespaces", new ChannelsInfoType(true)));
        addSubcommand(new SubCommand<>("alts", new AltsInfoType(api.players)));

        addSubcommand(new TopTimesInfoType(api));
    }

    private static class SubCommand<T> extends CommandDsl {

        public SubCommand(@NotNull String name, @NotNull PlayerInfoType<T> type) {
            super(name);

            category = CommandCategory.HIDDEN;

            setCondition(staffPerm(Permission.GENERIC_STAFF));

            var arg = type.getArgument();

            addSyntax(playerOnly((player, context) -> {
                var target = context.get(arg);

                if (target == null) {
                    player.sendMessage("Player with name %s not found".formatted(context.getRaw(arg)));
                    return;
                }

                type.execute(player, target);
            }), arg);
        }
    }
}
