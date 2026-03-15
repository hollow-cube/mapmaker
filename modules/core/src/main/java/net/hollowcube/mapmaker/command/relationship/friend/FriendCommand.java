package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;

public class FriendCommand extends CommandDsl {

    public FriendCommand(PlayerService playerService, MapService mapService, SessionManager sessionManager) {
        super("friend");

        this.description = "A command that lets you interact with your friend relationships (add/remove/list)";
        this.category = CommandCategories.SOCIAL;

        this.addSubcommand(new FriendAddCommand(playerService));
        this.addSubcommand(new FriendRemoveCommand(playerService));
        this.addSubcommand(new FriendListCommand(playerService, mapService, sessionManager));
        this.addSubcommand(new FriendRequestCommand(playerService));
    }
}
