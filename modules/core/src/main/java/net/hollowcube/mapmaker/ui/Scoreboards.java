package net.hollowcube.mapmaker.ui;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;

public class Scoreboards {
    private static Sidebar lobbyScoreboard;

    public static void init() {
        lobbyScoreboard = createLobbyScoreboard();
    }

    private static Sidebar createLobbyScoreboard() {
        Sidebar lobbySidebar = new Sidebar(Component.text("lobby"));

        lobbySidebar.setTitle(Component.translatable("scoreboard.title"));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lone", Component.translatable("scoreboard.line"), -1));
        return lobbySidebar;
    }

    public static void showPlayerLobbyScoreboard(Player player) {
        lobbyScoreboard.addViewer(player);
    }

    //TODO update scoreboards that need frequent updating every tick(?) async
    private void tick() {
        updateLobbyScoreboard();
    }

    private void updateLobbyScoreboard() {
        //TODO updates player count
    }
}
