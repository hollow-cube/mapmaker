package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;

public class FriendCommand extends CommandDsl {

    public FriendCommand(
        @NotNull ApiClient api,
        @NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull SessionManager sessionManager) {
        super("friend");

        this.description = "A command that lets you interact with your friend relationships (add/remove/list)";
        this.category = CommandCategories.SOCIAL;

        this.addSubcommand(new FriendAddCommand(api.players, playerService));
        this.addSubcommand(new FriendRemoveCommand(api.players, playerService));
        this.addSubcommand(new FriendListCommand(api, playerService, mapService, sessionManager));
        this.addSubcommand(new FriendRequestCommand(api.players, playerService));
    }
}
