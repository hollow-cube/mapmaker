package net.hollowcube.mapmaker.hub.feature.leaderboard;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.mapmaker.hub.entity.NpcTextModel;
import net.hollowcube.ipc.map.LeaderboardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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

    public void setTitle(@NotNull Component title) {
        titleEntity.getEntityMeta().setText(title);
    }

    public void setSubtitle(@NotNull Component subtitle) {
        subtitleEntity.getEntityMeta().setText(subtitle);
    }

    public void setUpdated(@NotNull Component updated) {
        updatedEntity.getEntityMeta().setText(updated);
    }

    public void setData(@NotNull Function<UUID, DisplayName> nameFunc, @NotNull LeaderboardData data) {
        this.data = data;

        entriesEntity.getEntityMeta().setText(buildTop10(nameFunc, data)
                .appendNewline()
                .append(Component.text("Your Score: ---")));
    }

    public void setEntriesRaw(@NotNull Component entriesText) {
        entriesEntity.getEntityMeta().setText(entriesText);
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

    private @NotNull Component buildTop10(@NotNull Function<UUID, DisplayName> nameFunc, @NotNull LeaderboardData data) {
        List<Component> names = new ArrayList<>();

        // Compute the target width of each line
        int maxWidth = 0;
        for (var entry : data.top()) {
            var name = LanguageProviderV2.translate(nameFunc.apply(entry.player()).render());
            names.add(name);
            maxWidth = Math.max(maxWidth, measureLine(name, entry.score(), entry.rank()));
        }

        // Rebuild each line properly with the known length
        var result = Component.text();
        for (int i = 0; i < data.top().size(); i++) {
            var entry = data.top().get(i);
            result.append(buildLine(names.get(i), entry.score(), entry.rank(), 125, true)).appendNewline();
        }
        for (int i = data.top().size(); i < 10; i++) {
            result.append(buildLine(Component.text("............................"), 0, i + 1, 125, true)).appendNewline();
        }
        return result.build();
    }

    private int measureLine(@NotNull Component playerName, long score, int rank) {
        var plainName = PlainTextComponentSerializer.plainText().serialize(playerName);
        return FontUtil.measureText(String.format("#%d%s%d", rank, plainName, score));
    }

    private @NotNull Component buildLine(@NotNull Component playerName, long score, int rank, int targetSize, boolean trueCenter) {
        var plainName = PlainTextComponentSerializer.plainText().serialize(playerName);
        var padding = (targetSize - measureLine(playerName, score, rank));

        int leftPadding;
        if (trueCenter) {
            leftPadding = (int) Math.ceil((targetSize / 2.0) - (FontUtil.measureText(plainName) / 2.0) - FontUtil.measureText("#" + rank));
        } else {
            leftPadding = (int) Math.ceil(padding / 2.0);
        }

        return Component.text("#" + rank)
                .append(Component.text(FontUtil.computeOffset(leftPadding)))
                .append(playerName)
                .append(Component.text(FontUtil.computeOffset(padding - leftPadding)))
                .append(Component.text(score));
    }
}
