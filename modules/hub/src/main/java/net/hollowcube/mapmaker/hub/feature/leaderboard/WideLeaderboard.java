package net.hollowcube.mapmaker.hub.feature.leaderboard;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.entity.NpcTextModel;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WideLeaderboard {

    // Config parameters
    // These can reasonably be changed to adjust the model

    private static final double SCREEN_ANGLE = 10; // The angle of the screen
    private static final double CENTER_BIAS = 0.5; // Higher value moves the text closer to together. Used for manual horizontal alignment
    private static final double TEXT_SHIFT = 1.6; // Shifts the text up/down the screen along the angled axis. Used for manual vertical alignment
    private static final double TITLE_SHIFT = 6.6;
    private static final double SUBTITLE_SHIFT = TITLE_SHIFT - 0.2;

    // Math Constants
    // These probably should not be changed.

    private static final double SCREEN_WIDTH = 11; // The width of the inner part of the screen model
    private static final double MODEL_SCALE = 16;
    private static final double TEXT_SCALE = 1.5;
    private static final double TITLE_SCALE = 2.25;
    private static final double SUBTITLE_SCALE = 1.25;

    private static final float[] ROTATION_QUAT = new Quaternion(new Vec(1, 0, 0).normalize(),
            Math.toRadians(SCREEN_ANGLE)).into();


    private final NpcItemModel screenModel = new NpcItemModel();
    private final NpcTextModel lbTextLeft = new NpcTextModel();
    private final NpcTextModel lbTitleLeft = new NpcTextModel();
    private final NpcTextModel lbSubtitleLeft = new NpcTextModel();
    private final NpcTextModel lbTextRight = new NpcTextModel();
    private final NpcTextModel lbTitleRight = new NpcTextModel();
    private final NpcTextModel lbSubtitleRight = new NpcTextModel();

    private LeaderboardData leftData = new LeaderboardData(List.of(
            new LeaderboardData.Entry("notmattw", 50, 1),
            new LeaderboardData.Entry("Ossipago1", 49, 2),
            new LeaderboardData.Entry("Ontal", 45, 3),
            new LeaderboardData.Entry("HammSamichz", 44, 4),
            new LeaderboardData.Entry("SethPRG", 43, 5),
            new LeaderboardData.Entry("Nixotica", 40, 6),
            new LeaderboardData.Entry("ArcaneWarrior", 32, 7),
            new LeaderboardData.Entry("Kha0x", 23, 8),
            new LeaderboardData.Entry("YouAreRexist", 15, 9),
            new LeaderboardData.Entry("MMMMMMMMMMMMMMM", 999, 10)
    ), null);

    public WideLeaderboard() {
        screenModel.setModel(Material.STICK, 4);
        screenModel.getEntityMeta().setScale(new Vec(MODEL_SCALE));
        screenModel.getEntityMeta().setLeftRotation(ROTATION_QUAT);

        initTextEntity(lbTextLeft, true, TEXT_SCALE, TEXT_SHIFT);
        initTextEntity(lbTextRight, false, TEXT_SCALE, TEXT_SHIFT);
        initTextEntity(lbTitleLeft, true, TITLE_SCALE, TITLE_SHIFT);
        initTextEntity(lbTitleRight, false, TITLE_SCALE, TITLE_SHIFT);
        initTextEntity(lbSubtitleLeft, true, SUBTITLE_SCALE, SUBTITLE_SHIFT);
        initTextEntity(lbSubtitleRight, false, SUBTITLE_SCALE, SUBTITLE_SHIFT);

        lbTextLeft.getEntityMeta().setText(buildTop10(leftData)
                .appendNewline()
                .append(buildLine(new LeaderboardData.Entry("You", 1, 55234), 175)));
        lbTitleLeft.getEntityMeta().setText(Component.text("ᴍᴀᴘѕ ʙᴇᴀᴛᴇɴ", NamedTextColor.GOLD));
        lbSubtitleLeft.getEntityMeta().setText(Component.text("ᴀʟʟ ᴛɪᴍᴇ", NamedTextColor.GRAY));

        lbTextRight.getEntityMeta().setText(buildTop10(leftData)
                .appendNewline()
                .append(buildLine(new LeaderboardData.Entry("You", 1, 55234), 175)));
        lbTitleRight.getEntityMeta().setText(Component.text("ᴛᴏᴘ ᴛɪᴍᴇѕ", NamedTextColor.GOLD));
        lbSubtitleRight.getEntityMeta().setText(Component.text("ᴀʟʟ ᴛɪᴍᴇ", NamedTextColor.GRAY));
    }

    public void setInstance(@NotNull Instance instance, @NotNull Point position) {
        screenModel.setInstance(instance, new Pos(position, 0, 0)).join();
        lbTextLeft.setInstance(instance, new Pos(position, 0, 0)).join();
        lbTitleLeft.setInstance(instance, new Pos(position, 0, 0)).join();
        lbSubtitleLeft.setInstance(instance, new Pos(position, 0, 0)).join();
        lbTextRight.setInstance(instance, new Pos(position, 0, 0)).join();
        lbTitleRight.setInstance(instance, new Pos(position, 0, 0)).join();
        lbSubtitleRight.setInstance(instance, new Pos(position, 0, 0)).join();
    }

    private void initTextEntity(@NotNull NpcTextModel entity, boolean isLeft, double scale, double shift) {
        var meta = entity.getEntityMeta();
        meta.setBackgroundColor(0);
        meta.setScale(new Vec(scale));
        meta.setLeftRotation(ROTATION_QUAT);

        // The math here (because i will forget) is the following:
        // X: The text is centered in the displays so we need to put it at 1/4 of the width of the screen to be aligned with the other one.
        //    however it looked kinda strange so i did a completely arbitrary subtraction of 0.5 to bias the text towards the center.
        // Y: We need to shift the text up a tiny bit to center it in the screen. The actual shift value is completely arbitrary.
        //    The value is computed using the angle of the screen to get the Y value to shift along the angled axis (most basic trig).
        // Z: Same as Y but for the other axis.
        meta.setTranslation(new Vec(
                (isLeft ? -1 : 1) * (SCREEN_WIDTH - CENTER_BIAS) / 4,
                (shift) * Math.cos(Math.toRadians(SCREEN_ANGLE)),
                (shift) * Math.tan(Math.toRadians(SCREEN_ANGLE))
        ));
    }

    private @NotNull Component buildTop10(@NotNull LeaderboardData data) {
        // Compute the target width of each line
        int maxWidth = 0;
        for (var entry : data.top()) {
            maxWidth = Math.max(maxWidth, measureLine(entry));
        }

        // Rebuild each line properly with the known length
        var result = Component.text();
        for (var entry : data.top()) {
            result.append(buildLine(entry, 175)).appendNewline();
        }
        return result.build();
    }

    private int measureLine(@NotNull LeaderboardData.Entry entry) {
        return FontUtil.measureText(String.format("#%d%s%d", entry.rank(), entry.player(), entry.score())) + 50;
    }

    private @NotNull Component buildLine(@NotNull LeaderboardData.Entry entry, int targetSize) {
        var halfPadding = (targetSize - measureLine(entry)) / 2.0;
        return Component.text("#" + entry.rank())
                .append(Component.text(FontUtil.computeOffset((int) Math.ceil(halfPadding))))
                .append(Component.text(entry.player()))
                .append(Component.text(FontUtil.computeOffset((int) Math.floor(halfPadding))))
                .append(Component.text(entry.score()));
    }
}
