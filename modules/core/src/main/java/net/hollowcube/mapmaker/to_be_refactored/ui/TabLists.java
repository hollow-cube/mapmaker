package net.hollowcube.mapmaker.to_be_refactored.ui;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ScoreboardObjectivePacket;
import net.minestom.server.scoreboard.TabList;

public class TabLists {
    // Static tablists for server-wide use
    private static TabList globalTabList;

    public static void init() {
        globalTabList = createGlobalTabList();
    }

    private static TabList createGlobalTabList() {
        TabList globalTabList = new TabList("globalTab", ScoreboardObjectivePacket.Type.HEARTS);

        globalTabList.setHeader(Component.text("HollowCube"));
        globalTabList.setFooter(Component.text("hollowcube.net"));

        return globalTabList;
    }

    public static void showPlayerGlobalTabList(Player player) {
        globalTabList.addViewer(player);
    }
}
