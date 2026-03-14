package net.hollowcube.mapmaker.hub.feature.leaderboard;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(HubFeature.class)
public class MainLeaderboardFeature implements HubFeature {
    private static final Logger logger = LoggerFactory.getLogger(MainLeaderboardFeature.class);

    private static final TaskSchedule SCHEDULE = TaskSchedule.tick(60 * 20); // 1 minute

    private @UnknownNullability Leaderboard2 parkourLeaderboard;
    private @UnknownNullability Leaderboard2 buildingLeaderboard;

    @Override
    public void load(MapServer server, HubMapWorld world) {
        var mapService = server.mapService();
        var playerService = server.playerService();
        parkourLeaderboard = new Leaderboard2(
                () -> mapService.getGlobalLeaderboard(MapService.LEADERBOARD_MAPS_BEATEN, null),
                playerId -> mapService.getGlobalLeaderboard(MapService.LEADERBOARD_MAPS_BEATEN, playerId).player().score(),
                () -> mapService.getGlobalLeaderboard(MapService.LEADERBOARD_TOP_TIMES, null),
                playerId -> mapService.getGlobalLeaderboard(MapService.LEADERBOARD_TOP_TIMES, playerId).player().score(),
                playerId -> playerService.getPlayerDisplayName2(playerId).build(),
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

        parkourLeaderboard.setInstance(world.instance(), new Pos(6, 39, -22.5, 90, 0));
        buildingLeaderboard.setInstance(world.instance(), new Pos(6, 39, 23.5, 90, 0));

        FutureUtil.submitVirtual(() -> parkourLeaderboard.update());
        FutureUtil.submitVirtual(() -> buildingLeaderboard.update());

        server.scheduler().scheduleTask(this::update, SCHEDULE, SCHEDULE);
    }

    // Called once every minute to update the leaderboards (tick up the time since update then update if necessary)
    public void update() {
        FutureUtil.submitVirtual(() -> {
            parkourLeaderboard.update();
            buildingLeaderboard.update();
        });
    }

}
