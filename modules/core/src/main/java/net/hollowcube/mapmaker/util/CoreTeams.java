package net.hollowcube.mapmaker.util;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamManager;

// Utility class for managing teams between maps and hubs.
public final class CoreTeams {
    private static final TeamManager TEAM_MANAGER = MinecraftServer.getTeamManager();

    // Default players
    public static final Team DEFAULT = TEAM_MANAGER.createBuilder("default")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS)
            .collisionRule(TeamsPacket.CollisionRule.NEVER)
            .seeInvisiblePlayers()
            .prefix(Component.text("\uF833")) // Tab ordering
            .build();

    // Green coloring rank
    public static final Team GREEN = TEAM_MANAGER.createBuilder("green")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS)
            .collisionRule(TeamsPacket.CollisionRule.NEVER)
            .seeInvisiblePlayers()
            .prefix(Component.text("\uF832")) // Tab ordering
            .build();

    // Blue coloring rank
    public static final Team CYAN = TEAM_MANAGER.createBuilder("cyan")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS)
            .collisionRule(TeamsPacket.CollisionRule.NEVER)
            .seeInvisiblePlayers()
            .prefix(Component.text("\uF831")) // Tab ordering
            .build();

    // Admin/red coloring in tab
    public static final Team RED = TEAM_MANAGER.createBuilder("red")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS)
            .collisionRule(TeamsPacket.CollisionRule.NEVER)
            .seeInvisiblePlayers()
            .prefix(Component.text("\uF830")) // Tab ordering
            .build();

}
