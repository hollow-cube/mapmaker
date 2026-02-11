package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;

public class PlayerInfoCommand extends CommandDsl {

    public PlayerInfoCommand(
        @NotNull PermManager permissions, @NotNull PlayerService players, @NotNull MapService mapService,
        @NotNull SessionManager sessionManager
    ) {
        super("playerinfo");

        category = CommandCategory.HIDDEN;

        setCondition(permissions.createPlatformCondition2(PlatformPerm.BAN_PLAYER));

        addSubcommand(new SubCommand<>(permissions, "general", new GeneralInfoType(sessionManager)));
        addSubcommand(new SubCommand<>(permissions, "channels", new ChannelsInfoType(false)));
        addSubcommand(new SubCommand<>(permissions, "channel_namespaces", new ChannelsInfoType(true)));
        addSubcommand(new SubCommand<>(permissions, "info_reports", new ReportsInfoType()));
        addSubcommand(new SubCommand<>(permissions, "alts", new AltsInfoType(players)));

        addSubcommand(new TopTimesInfoType(mapService, players));
    }

    private static class SubCommand<T> extends CommandDsl {

        public SubCommand(@NotNull PermManager permManager, @NotNull String name, @NotNull PlayerInfoType<T> type) {
            super(name);

            category = CommandCategory.HIDDEN;

            setCondition(permManager.createPlatformCondition2(PlatformPerm.BAN_PLAYER));

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
