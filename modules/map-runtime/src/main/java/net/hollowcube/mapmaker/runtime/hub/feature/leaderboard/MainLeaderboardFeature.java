package net.hollowcube.mapmaker.runtime.hub.feature.leaderboard;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.ipc.map.GlobalLeaderboard;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.runtime.freeform.FreeformMapWorld;
import net.hollowcube.mapmaker.runtime.hub.feature.HubFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

public class MainLeaderboardFeature implements HubFeature {
    private static final TaskSchedule SCHEDULE = TaskSchedule.tick(60 * 20); // 1 minute

    private Leaderboard2 parkourLeaderboard;
    private Leaderboard2 buildingLeaderboard;

    @Override
    public void load(@NotNull MapServer server, @NotNull FreeformMapWorld world) {
        var api = server.api();
        parkourLeaderboard = new Leaderboard2(
            () -> api.mapService.getGlobalLeaderboard(GlobalLeaderboard.MAPS_BEATEN, null),
            playerId -> api.mapService.getGlobalLeaderboard(GlobalLeaderboard.MAPS_BEATEN, playerId)
                .player(),
            () -> api.mapService.getGlobalLeaderboard(GlobalLeaderboard.TOP_TIMES, null),
            playerId -> api.mapService.getGlobalLeaderboard(GlobalLeaderboard.TOP_TIMES, playerId)
                .player(),
            playerId -> api.players.displayName(playerId).render(),
            10
        );
        buildingLeaderboard = new Leaderboard2(null, null, null, null, null, 10);

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
