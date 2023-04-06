package net.hollowcube.mapmaker.ui;

import net.hollowcube.mapmaker.model.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class Scoreboards {
    // Static scoreboards for server-wide use
    private static Sidebar lobbyScoreboard;

    // Non-static scoreboards are tracked per player as their active scoreboard
    private static Tag<Sidebar> activeScoreboard = Tag.Transient("activeScoreboard");

    public static void init() {
        lobbyScoreboard = createLobbyScoreboard();
    }

    private static Sidebar createLobbyScoreboard() {
        Sidebar lobbySidebar = new Sidebar(Component.text("lobby"));

        lobbySidebar.setTitle(Component.translatable("scoreboard.title"));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lobby_one", Component.translatable("scoreboard.line"), -1));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lobby_two", Component.translatable("scoreboard.lobby.info"), -2));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lobby_three", Component.translatable("scoreboard.lobby.online"), -3));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lobby_four", Component.translatable("scoreboard.line"), -4));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lobby_five", Component.translatable("scoreboard.IP"), -5));
        return lobbySidebar;
    }

    private static Sidebar createPlayingScoreboard(@NotNull MapData map) {
        Sidebar playingSidebar = new Sidebar(Component.text("playing"));

        var mapName = map.getName();
        var mapCreator = map.getOwner();
        // TODO show alias here instead if exists for map when implemented
        var mapId = map.getPublishedId();

        playingSidebar.setTitle(Component.translatable("scoreboard.title"));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("playing_one", Component.translatable("scoreboard.line"), -1));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("playing_two", Component.translatable("scoreboard.playing.map", Component.text(mapName)), -2));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("playing_three", Component.translatable("scoreboard.playing.map.creator", Component.text(mapCreator)), -3));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("playing_four", Component.translatable("scoreboard.playing.map.ID", Component.text(mapId)), -4));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("playing_five", Component.translatable("scoreboard.line"), -5));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("playing_six", Component.translatable("scoreboard.IP"), -6));
        return playingSidebar;
    }

    private static Sidebar createEditingScoreboard(@NotNull Player player, @NotNull MapData map) {
        Sidebar editingSidebar = new Sidebar(Component.text("editing"));

        var mapName = map.getName();
        var mapCreator = map.getOwner();

        editingSidebar.setTitle(Component.translatable("scoreboard.title"));
        editingSidebar.createLine(new Sidebar.ScoreboardLine("editing_one", Component.translatable("scoreboard.line"), -1));
        editingSidebar.createLine(new Sidebar.ScoreboardLine("editing_two", Component.translatable("scoreboard.editing.map", Component.text(mapName)), -2));
        if (player.getUuid().toString().equals(mapCreator)) {
            editingSidebar.createLine(new Sidebar.ScoreboardLine("editing_three", Component.translatable("scoreboard.editing.map.permission", Component.text("Owner")), -3));
        } else { //TODO redo this permission section when trusted members and other relation groups are added to editing maps
            editingSidebar.createLine(new Sidebar.ScoreboardLine("editing_three", Component.translatable("scoreboard.editing.map.permission", Component.text("Builder")), -3));
        }
        editingSidebar.createLine(new Sidebar.ScoreboardLine("editing_four", Component.translatable("scoreboard.line"), -4));
        editingSidebar.createLine(new Sidebar.ScoreboardLine("editing_five", Component.translatable("scoreboard.IP"), -5));
        return editingSidebar;
    }

    public static void showPlayerLobbyScoreboard(Player player) {
        lobbyScoreboard.addViewer(player);
    }

    public static void showPlayerPlayingScoreboard(@NotNull Player player, @NotNull MapData map) {
        if (player.getTag(activeScoreboard) != null) {
            // If it no longer has any viewers does it auto free due to out of scope?
            player.getTag(activeScoreboard).removeViewer(player);
        }
        var playingScoreboard = createPlayingScoreboard(map);
        playingScoreboard.addViewer(player);
    }

    public static void showPlayerEditingScoreboard(@NotNull Player player, @NotNull MapData map) {
        if (player.getTag(activeScoreboard) != null) {
            // If it no longer has any viewers does it auto free due to out of scope?
            player.getTag(activeScoreboard).removeViewer(player);
        }
        var editingScoreboard = createEditingScoreboard(player, map);
        editingScoreboard.addViewer(player);
    }

    public static void hidePlayerScoreboard(@NotNull Player player) {
        player.getTag(activeScoreboard).removeViewer(player);
    }

    //TODO update scoreboards that need frequent updating every tick(?) async
    private void tick() {
        updateLobbyScoreboard();
    }

    private void updateLobbyScoreboard() {
        // TODO get players across server, we don't have method for that yet afaik
        lobbyScoreboard.updateLineContent("lobby_three", Component.translatable("scoreboard.lobby.online")
                .append(Component.text("0")));
    }
}
