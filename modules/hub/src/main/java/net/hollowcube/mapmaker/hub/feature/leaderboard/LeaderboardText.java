package net.hollowcube.mapmaker.hub.feature.leaderboard;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.hub.entity.NpcTextModel;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class LeaderboardText {

    private static final double TEXT_SHIFT = 1.6; // Shifts the text up/down the screen along the angled axis. Used for manual vertical alignment
    private static final double TITLE_SHIFT = 6.6;
    private static final double SUBTITLE_SHIFT = TITLE_SHIFT - 0.2;

    private static final double TEXT_SCALE = 1.5;
    private static final double TITLE_SCALE = 2.25;
    private static final double SUBTITLE_SCALE = 1.25;

    private final NpcTextModel entriesEntity = new NpcTextModel();
    private final NpcTextModel titleEntity = new NpcTextModel();
    private final NpcTextModel subtitleEntity = new NpcTextModel();
    private final NpcTextModel updatedEntity = new NpcTextModel();

    private LeaderboardData data = null;

    public LeaderboardText(double horizontalOffset, double screenAngle) {
        initTextEntity(entriesEntity, horizontalOffset, TEXT_SCALE, TEXT_SHIFT, screenAngle);
        initTextEntity(titleEntity, horizontalOffset, TITLE_SCALE, TITLE_SHIFT, screenAngle);
        initTextEntity(subtitleEntity, horizontalOffset, SUBTITLE_SCALE, SUBTITLE_SHIFT, screenAngle);
        initTextEntity(updatedEntity, horizontalOffset, SUBTITLE_SCALE, TEXT_SHIFT + 0.5, screenAngle);
    }

    public void setData(@NotNull LeaderboardData data) {
        this.data = data;

        entriesEntity.getEntityMeta().setText(buildTop10(data)
                .appendNewline()
                .append(Component.text("Your Score: 1")));
        titleEntity.getEntityMeta().setText(Component.text("ᴍᴀᴘѕ ʙᴇᴀᴛᴇɴ", NamedTextColor.GOLD));
        subtitleEntity.getEntityMeta().setText(Component.text("ᴀʟʟ ᴛɪᴍᴇ", NamedTextColor.GRAY));
        updatedEntity.getEntityMeta().setText(Component.text("ᴜᴘᴅᴀᴛᴇᴅ " + FontUtil.rewrite("smallnums", "5") + "ᴍ ᴀɢᴏ", NamedTextColor.DARK_GRAY));
    }

    public @NotNull CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos pos) {
        return CompletableFuture.allOf(
                entriesEntity.setInstance(instance, pos),
                titleEntity.setInstance(instance, pos),
                subtitleEntity.setInstance(instance, pos),
                updatedEntity.setInstance(instance, pos)
        );
    }

    private void initTextEntity(@NotNull NpcTextModel entity, double horizontalOffset, double scale, double shift, double screenAngle) {
        var meta = entity.getEntityMeta();
        meta.setBackgroundColor(0);
        meta.setScale(new Vec(scale));
        meta.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(screenAngle)).into());

        // The math here (because i will forget) is the following:
        // Y: We need to shift the text up a tiny bit to center it in the screen. The actual shift value is completely arbitrary.
        //    The value is computed using the angle of the screen to get the Y value to shift along the angled axis (most basic trig).
        // Z: Same as Y but for the other axis.
        meta.setTranslation(new Vec(
                horizontalOffset,
                (shift) * Math.cos(Math.toRadians(screenAngle)),
                (shift) * Math.tan(Math.toRadians(screenAngle))
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
            result.append(buildLine(entry, 125, true)).appendNewline();
        }
        return result.build();
    }

    private int measureLine(@NotNull LeaderboardData.Entry entry) {
        return FontUtil.measureText(String.format("#%d%s%d", entry.rank(), entry.player(), entry.score()));
    }

    private @NotNull Component buildLine(@NotNull LeaderboardData.Entry entry, int targetSize, boolean trueCenter) {
        var padding = (targetSize - measureLine(entry));

        int leftPadding;
        if (trueCenter) {
            leftPadding = (int) Math.ceil((targetSize / 2.0) - (FontUtil.measureText(entry.player()) / 2.0) - FontUtil.measureText("#" + entry.rank()));
        } else {
            leftPadding = (int) Math.ceil(padding / 2.0);
        }

        return Component.text("#" + entry.rank())
                .append(Component.text(FontUtil.computeOffset(leftPadding)))
                .append(Component.text(entry.player()))
                .append(Component.text(FontUtil.computeOffset(padding - leftPadding)))
                .append(Component.text(entry.score()));
    }
}
