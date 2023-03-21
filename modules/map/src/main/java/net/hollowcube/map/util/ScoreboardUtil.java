package net.hollowcube.map.util;

import net.hollowcube.map.world.MapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardUtil {
    public static Tag<Boolean> NoScoreboards = Tag.Boolean("NoScoreboards");

    public static void sendLobbyScoreboard(@NotNull Player player) { //TODO move into hub module
        if (player.getTag(NoScoreboards)) {
            return;
        }
        Sidebar lobbySidebar = new Sidebar(Component.text("Map Maker Lobby"));

        int numPlayers = player.getInstance().getPlayers().size();

        List<Component> args = new ArrayList<>();

        // set args(0) and args(1)
        args.add(Component.text("01"));
        args.add(Component.text(numPlayers));

        // set title and add lines
        lobbySidebar.setTitle(Component.translatable("scoreboard.title"));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lone", Component.translatable("scoreboard.line"), -1));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("ltwo", Component.translatable("scoreboard.lobby.info", args.get(0)), -2));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lthree", Component.translatable("scoreboard.lobby.online", args.get(1)), -3));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lfour", Component.translatable("scoreboard.line"), -4));
        lobbySidebar.createLine(new Sidebar.ScoreboardLine("lfive", Component.translatable("scoreboard.IP"), -5));
        lobbySidebar.addViewer(player);
    }

    public static void sendPlayingScoreboard(@NotNull Player player) {
        if (player.getTag(NoScoreboards)) { return; }
        Sidebar playingSidebar = new Sidebar(Component.text("Map Maker Play Maps"));

        var map = MapWorld.fromInstance(player.getInstance()).map().getName();
        var mapCreator = MapWorld.fromInstance(player.getInstance()).map().getOwner();
        //TODO implement UUID to name cache system for var mapCreator
        var mapID = MapWorld.fromInstance(player.getInstance()).map().getPublishedId();

        List<Component> args = new ArrayList<>();

        // set args(0), args(1), args(2)
        args.add(Component.text(map));
        args.add(Component.text(mapCreator));
        args.add(Component.text(mapID));

        playingSidebar.setTitle(Component.translatable("scoreboard.title"));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("pone", Component.translatable("scoreboard.line"), -1));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("ptwo", Component.translatable("scoreboard.playing.map", args.get(0)), -2));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("pthree", Component.translatable("scoreboard.playing.map.creator", args.get(1)), -3));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("pfour", Component.translatable("scoreboard.playing.map.ID", args.get(2)), -4));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("pfive", Component.translatable("scoreboard.line"), -5));
        playingSidebar.createLine(new Sidebar.ScoreboardLine("psix", Component.translatable("scoreboard.IP"), -6));
        playingSidebar.addViewer(player);
    }

    public static void sendEditingScoreboard(@NotNull Player player) {
        if (player.getTag(NoScoreboards)) { return; }
        Sidebar editingSidebar = new Sidebar(Component.text("Map Maker Create Maps"));

        var map = MapWorld.fromInstance(player.getInstance()).map().getName();
        var mapCreator = MapWorld.fromInstance(player.getInstance()).map().getOwner();

        List<Component> args = new ArrayList<>();

        // set args(0), args(1), args(2)
        args.add(Component.text(map));
        args.add(Component.text("Owner"));
        args.add(Component.text("Builder"));

        editingSidebar.setTitle(Component.translatable("scoreboard.title"));
        editingSidebar.createLine(new Sidebar.ScoreboardLine("eone", Component.translatable("scoreboard.line"), -1));
        editingSidebar.createLine(new Sidebar.ScoreboardLine("etwo", Component.translatable("scoreboard.editing.map", args.get(0)), -2));
        if (player.getUuid().toString().equals(mapCreator)) {
            editingSidebar.createLine(new Sidebar.ScoreboardLine("ethree", Component.translatable("scoreboard.editing.map.permission", args.get(1)), -3));
        } else { //TODO redo this permission section when trusted members and other relation groups are added to editing maps
            editingSidebar.createLine(new Sidebar.ScoreboardLine("ethree", Component.translatable("scoreboard.editing.map.permission", args.get(2)), -3));
        }
        editingSidebar.createLine(new Sidebar.ScoreboardLine("efour", Component.translatable("scoreboard.line"), -4));
        editingSidebar.createLine(new Sidebar.ScoreboardLine("efive", Component.translatable("scoreboard.IP"), -5));
        editingSidebar.addViewer(player);
        editingSidebar.removeViewer(player);
    }

    public static void removeScoreboard(@NotNull Player player) { //TODO remake this garbage entirely
        //literally anything else I tried didn't work, so here's my budget solution lol
        Sidebar removeSidebar = new Sidebar(Component.text("certified mattw moment"));
        removeSidebar.setTitle(Component.text(""));
        removeSidebar.createLine(new Sidebar.ScoreboardLine("rone", Component.text(""), -1));
        removeSidebar.createLine(new Sidebar.ScoreboardLine("rtwo", Component.text(""), -2));
        removeSidebar.createLine(new Sidebar.ScoreboardLine("rthree", Component.text(""), -3));
        removeSidebar.createLine(new Sidebar.ScoreboardLine("rfour", Component.text(""), -4));
        removeSidebar.createLine(new Sidebar.ScoreboardLine("rfive", Component.text(""), -5));
        removeSidebar.createLine(new Sidebar.ScoreboardLine("rsix", Component.text(""), -6));
        removeSidebar.removeViewer(player);
    }
}
