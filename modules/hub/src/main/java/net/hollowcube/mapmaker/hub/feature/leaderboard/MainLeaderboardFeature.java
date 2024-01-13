package net.hollowcube.mapmaker.hub.feature.leaderboard;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(HubFeature.class)
public class MainLeaderboardFeature implements HubFeature {
    private static final Logger logger = LoggerFactory.getLogger(MainLeaderboardFeature.class);

    private static final TaskSchedule SCHEDULE = TaskSchedule.tick(60 * 20); // 1 minute

    private Leaderboard2 parkourLeaderboard;
    private Leaderboard2 buildingLeaderboard;

    private final Leaderboard1 testlb = new Leaderboard1(0);
    private final Leaderboard1 testlb2 = new Leaderboard1(0);

    @Override
    public void init(@NotNull HubServer hub) {
        parkourLeaderboard = new Leaderboard2(
                () -> hub.mapService().getGlobalLeaderboard(MapService.LEADERBOARD_MAPS_BEATEN, null),
                playerId -> hub.mapService().getGlobalLeaderboard(MapService.LEADERBOARD_MAPS_BEATEN, playerId).player().score(),
                () -> hub.mapService().getGlobalLeaderboard(MapService.LEADERBOARD_TOP_TIMES, null),
                playerId -> hub.mapService().getGlobalLeaderboard(MapService.LEADERBOARD_TOP_TIMES, playerId).player().score(),
                playerId -> hub.playerService().getPlayerDisplayName2(playerId).build(),
                10);
        buildingLeaderboard = new Leaderboard2(
                null, null,
                null, null,
                null, 10);

        assert parkourLeaderboard.left != null;
        parkourLeaderboard.left.setTitle(Component.text("ᴍᴀᴘѕ ʙᴇᴀᴛᴇɴ", NamedTextColor.GOLD));
        parkourLeaderboard.left.setSubtitle(Component.text("ᴀʟʟ ᴛɪᴍᴇ", NamedTextColor.GRAY));

        assert parkourLeaderboard.right != null;
        parkourLeaderboard.right.setTitle(Component.text("ᴛᴏᴘ ᴛɪᴍᴇѕ", NamedTextColor.GOLD));
        parkourLeaderboard.right.setSubtitle(Component.text("ᴀʟʟ ᴛɪᴍᴇ", NamedTextColor.GRAY));

        parkourLeaderboard.update();
        parkourLeaderboard.setInstance(hub.instance(), new Pos(6, 39, -22.5, 90, 0));
        buildingLeaderboard.update();
        buildingLeaderboard.setInstance(hub.instance(), new Pos(6, 39, 23.5, 90, 0));

        testlb.setInstance(hub.instance(), new Pos(-25.5, 41, 53.5, 90 + 45, 0));
        testlb2.setInstance(hub.instance(), new Pos(-49.5, 41, 53.5, 90 + 45 + 90, 0));

        hub.scheduler().scheduleTask(this::update, SCHEDULE, SCHEDULE);
    }

    // Called once every minute to update the leaderboards (tick up the time since update then update if necessary)
    public void update() {
        parkourLeaderboard.update();
        buildingLeaderboard.update();
    }

}
