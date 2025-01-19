package net.hollowcube.mapmaker.map.entity.marker;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.util.LeaderboardDisplay;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.NotNull;

public class MapLeaderboardMarkerHandler extends MarkerHandler {
    private static final int ONE_MINUTE_TICKS = 60 * 20;

    public static final String ID = "mapmaker:leaderboard";

    private LeaderboardDisplay leaderboard;
    private int ticksUntilUpdate = ONE_MINUTE_TICKS;

    public MapLeaderboardMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);

        var world = MapWorld.unsafeFromInstance(entity.getInstance());
        if (world == null) throw new RuntimeException("missing world");

        if (!world.map().isPublished() || world instanceof EditingMapWorld) {
            entity.setRegion(new Vec(-1.5, 0, -1.5), new Vec(1.5, 3.5, 1.5));
            return;
        }

        // Create actual leaderboard
        var data = entity.getMarkerData();
        var scale = data.getDouble("scale", 1) * 0.5;
        var facing = data.getString("facing", "follow");
        var hasBackground = data.getBoolean("background", true);
        this.leaderboard = new LeaderboardDisplay(entity,
                () -> world.server().mapService().getPlaytimeLeaderboard(world.map().id(), null),
                playerId -> world.server().mapService().getPlaytimeLeaderboard(world.map().id(), playerId).player().score(),
                playerId -> world.server().playerService().getPlayerDisplayName2(playerId).build(),
                0, 0, 0, scale);
        leaderboard.setPadding(true);
        if (hasBackground) leaderboard.entriesDisplay().setUseDefaultBackground(true);
        leaderboard.setTitle(Component.text(FontUtil.rewrite("small", "fastest times"), NamedTextColor.GOLD), 0.3 * scale);
        leaderboard.setSubtitle(Component.text("ᴀʟʟ ᴛɪᴍᴇ", NamedTextColor.GRAY), 0.3 * scale);
        leaderboard.editDisplays(meta -> {
            switch (facing) {
                case "north" -> meta.setLeftRotation(new Quaternion(new Vec(0, 1, 0).normalize(),
                        Math.toRadians(180)).into());
                case "south" -> {/* Do nothing*/}
                case "east" -> meta.setLeftRotation(new Quaternion(new Vec(0, 1, 0).normalize(),
                        Math.toRadians(90)).into());
                case "west" -> meta.setLeftRotation(new Quaternion(new Vec(0, 1, 0).normalize(),
                        Math.toRadians(-90)).into());
                default -> meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
            }
        });
        leaderboard.setScoreFormatter(score -> NumberUtil.formatMapPlaytime(score, true));
        leaderboard.setTargetWidth(150);
        leaderboard.setTrueCenter(false);
        entity.scheduleNextTick(ignored -> leaderboard.setInstance(world.instance(), entity.getPosition()));

        FutureUtil.submitVirtual(() -> leaderboard.update());
    }

    @Override
    protected void onTick() {
        super.onTick();

        ticksUntilUpdate--;
        if (ticksUntilUpdate == 0) {
            FutureUtil.submitVirtual(() -> {
                leaderboard.update();
                entity.getViewers().forEach(leaderboard::update);
                ticksUntilUpdate = ONE_MINUTE_TICKS;
            });
        }
    }

    @Override
    protected void addViewer(@NotNull Player player) {
        FutureUtil.submitVirtual(() -> leaderboard.update(player));
    }
}
