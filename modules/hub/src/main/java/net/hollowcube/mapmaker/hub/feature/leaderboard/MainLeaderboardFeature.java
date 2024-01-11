package net.hollowcube.mapmaker.hub.feature.leaderboard;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(HubFeature.class)
public class MainLeaderboardFeature implements HubFeature {
    private static final Logger logger = LoggerFactory.getLogger(MainLeaderboardFeature.class);

    private PlayerService playerService;

    private final Leaderboard2 parkourLeaderboard = new Leaderboard2(10);
    private final Leaderboard2 buildingLeaderboard = new Leaderboard2(10);

    private final Leaderboard1 testlb = new Leaderboard1(0);
    private final Leaderboard1 testlb2 = new Leaderboard1(0);

    @Override
    public void init(@NotNull HubServer hub) {
        playerService = hub.playerService();

        parkourLeaderboard.left.setTitle(Component.text("ᴍᴀᴘѕ ʙᴇᴀᴛᴇɴ", NamedTextColor.GOLD));
        parkourLeaderboard.left.setSubtitle(Component.text("ᴀʟʟ ᴛɪᴍᴇ", NamedTextColor.GRAY));
        parkourLeaderboard.left.setUpdated(Component.text("ᴜᴘᴅᴀᴛᴇᴅ -- ᴀɢᴏ", NamedTextColor.DARK_GRAY)); // FontUtil.rewrite("smallnums", "5")

        parkourLeaderboard.right.setTitle(Component.text("ᴛᴏᴘ ᴛɪᴍᴇѕ", NamedTextColor.GOLD));
        parkourLeaderboard.right.setSubtitle(Component.text("ᴀʟʟ ᴛɪᴍᴇ", NamedTextColor.GRAY));
        parkourLeaderboard.right.setUpdated(Component.text("ᴜᴘᴅᴀᴛᴇᴅ -- ᴀɢᴏ", NamedTextColor.DARK_GRAY));

        var mapsBeatenInitialData = hub.mapService().getGlobalLeaderboard(MapService.LEADERBOARD_MAPS_BEATEN, null);
        parkourLeaderboard.left.setData(playerService::getPlayerDisplayName2, mapsBeatenInitialData);
        var topTimesInitialData = hub.mapService().getGlobalLeaderboard(MapService.LEADERBOARD_TOP_TIMES, null);
        parkourLeaderboard.right.setData(playerService::getPlayerDisplayName2, topTimesInitialData);

        parkourLeaderboard.setInstance(hub.instance(), new Pos(6, 39, -22.5, 90, 0));
        buildingLeaderboard.setInstance(hub.instance(), new Pos(6, 39, 23.5, 90, 0));


        testlb.setInstance(hub.instance(), new Pos(-25.5, 41, 53.5, 90 + 45, 0));
        testlb2.setInstance(hub.instance(), new Pos(-49.5, 41, 53.5, 90 + 45 + 90, 0));
    }

}
