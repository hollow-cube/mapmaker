package net.hollowcube.mapmaker.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamManager;

// Utility class for managing teams between maps and hubs.
public final class CoreTeams {
    private static final TeamManager TEAM_MANAGER = MinecraftServer.getTeamManager();

    // Default players
    public static final Team DEFAULT = TEAM_MANAGER.createBuilder("default")
        .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
        .collisionRule(TeamsPacket.CollisionRule.NEVER)
        .allowFriendlyFire()
        .seeInvisiblePlayers()
        .build();

}
