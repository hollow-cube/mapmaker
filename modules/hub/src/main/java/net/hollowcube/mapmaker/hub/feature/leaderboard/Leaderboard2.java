package net.hollowcube.mapmaker.hub.feature.leaderboard;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.hollowcube.mapmaker.util.LeaderboardDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.hollowcube.mapmaker.util.LeaderboardDisplay.SUBTITLE_SCALE;
import static net.hollowcube.mapmaker.util.LeaderboardDisplay.initTextEntity;

public class Leaderboard2 {
    private static final double CENTER_BIAS = 0.5; // Higher value moves the text closer to together. Used for manual horizontal alignment
    private static final double SCREEN_WIDTH = 11; // The width of the inner part of the screen model
    private static final double MODEL_SCALE = 16;

    private static final Component NOW_TEXT = Component.text("ᴜᴘᴅᴀᴛᴇᴅ ᴊᴜѕᴛ ɴᴏᴡ", NamedTextColor.DARK_GRAY);

    private final NpcItemModel screenModel = new NpcItemModel();
    private final LeaderboardDisplay.TextDisplay updatedTextEntity = new LeaderboardDisplay.TextDisplay();

    public final LeaderboardDisplay left;
    public final LeaderboardDisplay right;

    private long refreshInterval = 15 * 60 * 1000; // 15 minutes
    private long lastRefresh = -1;

    public Leaderboard2(
            @Nullable Supplier<LeaderboardData> leftGlobalLeaderboardSupplier,
            @Nullable Function<String, Long> leftPlayerScoreSupplier,
            @Nullable Supplier<LeaderboardData> rightGlobalLeaderboardSupplier,
            @Nullable Function<String, Long> rightPlayerScoreSupplier,
            @NotNull Function<String, Component> displayNameSupplier,
            double screenAngle
    ) {
        // TODO(1.21.4)
//        screenModel.setModel(Material.STICK, 4);
        screenModel.getEntityMeta().setScale(new Vec(MODEL_SCALE));
        screenModel.getEntityMeta().setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(screenAngle)).into());
        screenModel.setStatic(true);

        initTextEntity(updatedTextEntity, 0, SUBTITLE_SCALE, 1.1, screenAngle);

        screenModel.setAddViewerHook(this::addViewerHook);

        // X: The text is centered in the displays so we need to put it at 1/4 of the width of the screen to be aligned with the other one.
        //    however it looked kinda strange so i did a completely arbitrary subtraction of 0.5 to bias the text towards the center.
        if (leftGlobalLeaderboardSupplier != null && leftPlayerScoreSupplier != null) {
            left = new LeaderboardDisplay(screenModel, leftGlobalLeaderboardSupplier, leftPlayerScoreSupplier, displayNameSupplier,
                    -(SCREEN_WIDTH - CENTER_BIAS) / 4, screenAngle, 1.6, 1);
        } else left = null;
        if (rightGlobalLeaderboardSupplier != null && rightPlayerScoreSupplier != null) {
            right = new LeaderboardDisplay(screenModel, rightGlobalLeaderboardSupplier, rightPlayerScoreSupplier, displayNameSupplier,
                    (SCREEN_WIDTH - CENTER_BIAS) / 4, screenAngle, 1.6, 1);
        } else right = null;
    }

    public void setInstance(@NotNull Instance instance, @NotNull Pos pos) {
        screenModel.setInstance(instance, pos);
        updatedTextEntity.setInstance(instance, pos);
        if (left != null) left.setInstance(instance, pos);
        if (right != null) right.setInstance(instance, pos);
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (lastRefresh == -1 || now - lastRefresh >= refreshInterval) {
            if (left != null) left.update();
            if (right != null) right.update();

            lastRefresh = now;
            updatedTextEntity.getEntityMeta().setText(NOW_TEXT);
        } else {
            long minutes = (now - lastRefresh) / 1000 / 60;
            updatedTextEntity.getEntityMeta().setText(Component.text("ᴜᴘᴅᴀᴛᴇᴅ ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(FontUtil.rewrite("smallnums", String.valueOf(minutes))))
                    .append(Component.text("ᴍ ᴀɢᴏ")));
        }
    }

    public void addViewerHook(@NotNull Consumer<Player> superFunc, @NotNull Player player) {
        superFunc.accept(player);

        FutureUtil.submitVirtual(() -> {
            if (left != null) left.update(player);
            if (right != null) right.update(player);
        });
    }
}
