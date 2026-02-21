package net.hollowcube.mapmaker.command.staff;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class SFindCommand extends CommandDsl {
    private final Argument<String> targetArg;

    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionManager sessionManager;

    public SFindCommand(
        @NotNull MapService mapService,
        @NotNull PlayerService playerService,
        @NotNull SessionManager sessionManager
    ) {
        super("sfind");
        this.mapService = mapService;
        this.playerService = playerService;
        this.sessionManager = sessionManager;

        category = CommandCategories.STAFF;
        description = "Find a player on the server";
        targetArg = CoreArgument.AnyOnlinePlayer("player", sessionManager)
            .description("The player you want to find");

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::handleFindPlayer), targetArg);
    }

    private void handleFindPlayer(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(targetArg);

        var presence = sessionManager.getPresence(target);
        if (presence == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.text(context.getRaw(targetArg))));
            return;
        }

        var targetName = playerService.getPlayerDisplayName2(target).build();
        switch (presence.type()) {
            case Presence.TYPE_MAPMAKER_HUB ->
                player.sendMessage(Component.translatable("command.sfind.result.hub", targetName));
            case Presence.TYPE_MAPMAKER_MAP -> {
                var map = mapService.getMap(target, presence.mapId());
                player.sendMessage(LanguageProviderV2.translateMultiMerged(
                    "command.sfind.result.map",
                    List.of(targetName, Component.text(map.name()), Component.text(presence.state()), Component.text(presence.instanceId()))
                ));
            }
            default -> player.sendMessage(Component.translatable("command.where.unknown", targetName));
        }
    }

}
