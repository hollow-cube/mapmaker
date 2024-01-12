package net.hollowcube.mapmaker.hub.feature.leaderboard;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.hub.entity.NpcTextModel;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a single display of a leaderboard, with the ability to update per player.
 *
 * <p>Requires a function to get 1. the global leaderboard data, 2. any player's score, 3. a players name from their uuid.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class LeaderboardDisplay {

    private static final double TEXT_SHIFT = 1.6; // Shifts the text up/down the screen along the angled axis. Used for manual vertical alignment
    private static final double TITLE_SHIFT = 6.6;
    private static final double SUBTITLE_SHIFT = TITLE_SHIFT - 0.2;

    private static final double TEXT_SCALE = 1.5;
    private static final double TITLE_SCALE = 2.25;
    static final double SUBTITLE_SCALE = 1.25;

    private static final int TEXT_METADATA_INDEX = 23;

    private final Viewable parent;

    private final Supplier<LeaderboardData> globalLeaderboardSupplier;
    private final Function<String, Long> playerScoreSupplier;
    private final Function<String, Component> displayNameSupplier;

    private final NpcTextModel entriesEntity = new NpcTextModel();
    private final NpcTextModel titleEntity = new NpcTextModel();
    private final NpcTextModel subtitleEntity = new NpcTextModel();
    private final NpcTextModel updatedEntity = new NpcTextModel();

    private LeaderboardData cachedData = null;
    private Component cachedTopTen = null;

    public LeaderboardDisplay(
            @NotNull Viewable parent,
            @NotNull Supplier<LeaderboardData> globalLeaderboardSupplier,
            @NotNull Function<String, Long> playerScoreSupplier,
            @NotNull Function<String, Component> displayNameSupplier
    ) {
        this(parent, globalLeaderboardSupplier, playerScoreSupplier, displayNameSupplier, 0, 0);
    }

    public LeaderboardDisplay(
            @NotNull Viewable parent,
            @NotNull Supplier<LeaderboardData> globalLeaderboardSupplier,
            @NotNull Function<String, Long> playerScoreSupplier,
            @NotNull Function<String, Component> displayNameSupplier,
            double horizontalOffset, double screenAngle
    ) {
        this.parent = parent;
        this.globalLeaderboardSupplier = globalLeaderboardSupplier;
        this.playerScoreSupplier = playerScoreSupplier;
        this.displayNameSupplier = displayNameSupplier;

        initTextEntity(entriesEntity, horizontalOffset, TEXT_SCALE, TEXT_SHIFT, screenAngle);
        initTextEntity(titleEntity, horizontalOffset, TITLE_SCALE, TITLE_SHIFT, screenAngle);
        initTextEntity(subtitleEntity, horizontalOffset, SUBTITLE_SCALE, SUBTITLE_SHIFT, screenAngle);

        // Disable updates for this entities metadata. We will update the default text and it will still be
        // sent to new viewers as soon as they view the entity first. However, all subsequent updates will be
        // sent per player so we do not want to notify them when we change the default.
        entriesEntity.getEntityMeta().setNotifyAboutChanges(false);
    }

    public @NotNull CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos pos) {
        return CompletableFuture.allOf(
                entriesEntity.setInstance(instance, pos),
                titleEntity.setInstance(instance, pos),
                subtitleEntity.setInstance(instance, pos),
                updatedEntity.setInstance(instance, pos)
        );
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

    @Blocking
    public void update() {
        cachedData = globalLeaderboardSupplier.get();
        cachedTopTen = buildTop10(displayNameSupplier, cachedData);

        // Set the default text for new viewers while we fetch their data.
        entriesEntity.getEntityMeta().setText(cachedTopTen
                .appendNewline().append(Component.text("You: --")));

        // Update for all the viewers
        entriesEntity.getViewers().forEach(this::update);
    }

    @Blocking
    public void update(@NotNull Player player) {
        if (cachedData == null) return;

        var playerId = player.getUuid().toString();
        long playerScore = cachedData.getScore(playerId);

        if (playerScore == -1) {
            playerScore = playerScoreSupplier.apply(playerId);
        }

        var content = cachedTopTen.appendNewline().append(Component.text("You: " + playerScore));
        if (playerScore != -1) content = content.append(Component.text(" (", NamedTextColor.GRAY)
                .append(Component.text("#1"))
                .append(Component.text(")", NamedTextColor.GRAY)));
        Map<Integer, Metadata.Entry<?>> metaUpdates = Map.of(TEXT_METADATA_INDEX, Metadata.Chat(content));
        player.sendPacket(new EntityMetaDataPacket(entriesEntity.getEntityId(), metaUpdates));
    }

    static void initTextEntity(@NotNull NpcTextModel entity, double horizontalOffset, double scale, double shift, double screenAngle) {
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

    private @NotNull Component buildTop10(@NotNull Function<String, Component> nameFunc, @NotNull LeaderboardData data) {
        List<Component> names = new ArrayList<>();

        // Compute the target width of each line
        int maxWidth = 0;
        for (var entry : data.top()) {
            var name = nameFunc.apply(entry.player());
            names.add(name);
            maxWidth = Math.max(maxWidth, measureLine(name, entry));
        }

        // Rebuild each line properly with the known length
        var result = Component.text();
        for (int i = 0; i < data.top().size(); i++) {
            var entry = data.top().get(i);
            result.append(buildLine(names.get(i), entry, 125, true))
                    .appendNewline();
        }
        for (int i = data.top().size(); i < 10; i++) {
            result.append(buildLine(Component.text("............................"),
                    new LeaderboardData.Entry("", 0, i + 1),
                    125, true)).appendNewline();
        }
        return result.build();
    }

    private int measureLine(@NotNull Component playerName, @NotNull LeaderboardData.Entry entry) {
        var plainName = PlainTextComponentSerializer.plainText().serialize(playerName);
        return FontUtil.measureText(String.format("#%d%s%d", entry.rank(), plainName, entry.score()));
    }

    private @NotNull Component buildLine(@NotNull Component playerName, @NotNull LeaderboardData.Entry entry, int targetSize, boolean trueCenter) {
        var plainName = PlainTextComponentSerializer.plainText().serialize(playerName);
        var padding = (targetSize - measureLine(playerName, entry));

        int leftPadding;
        if (trueCenter) {
            leftPadding = (int) Math.ceil((targetSize / 2.0) - (FontUtil.measureText(plainName) / 2.0) - FontUtil.measureText("#" + entry.rank()));
        } else {
            leftPadding = (int) Math.ceil(padding / 2.0);
        }

        return Component.text("#" + entry.rank())
                .append(Component.text(FontUtil.computeOffset(leftPadding)))
                .append(playerName)
                .append(Component.text(FontUtil.computeOffset(padding - leftPadding)))
                .append(Component.text(entry.score()));
    }
}
