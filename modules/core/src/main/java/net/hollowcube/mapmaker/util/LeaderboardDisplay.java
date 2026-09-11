package net.hollowcube.mapmaker.util;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.compat.axiom.AxiomPlayer;
import net.hollowcube.ipc.map.LeaderboardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * Represents a single display of a leaderboard, with the ability to update per player.
 *
 * <p>Requires a function to get 1. the global leaderboard data, 2. any player's score, 3. a players name from their uuid.</p>
 */
public class LeaderboardDisplay {

    private static final double TITLE_SHIFT = 5; // Shifts the text up/down the screen along the angled axis. Used for manual vertical alignment
    private static final double SUBTITLE_SHIFT = TITLE_SHIFT - 0.2;

    private static final double TEXT_SCALE = 1.5;
    private static final double TITLE_SCALE = 2.25;
    public static final double SUBTITLE_SCALE = 1.25;

    private static final Component HORIZONTAL_PADDING = Component.text(FontUtil.computeOffset(9));

    private final Viewable parent;

    private final Supplier<LeaderboardData> globalLeaderboardSupplier;
    private final Function<UUID, LeaderboardData.Entry> playerScoreSupplier;
    private final Function<UUID, Component> displayNameSupplier;

    private final TextDisplay entriesEntity = new TextDisplay() {
        @Override
        public void updateOldViewer(@NotNull Player player) {

        }
    };
    private final TextDisplay titleEntity = new TextDisplay();
    private final TextDisplay subtitleEntity = new TextDisplay();
    private final TextDisplay updatedEntity = new TextDisplay();

    private LeaderboardData cachedData = null;
    private Component cachedTopTen = null;

    private LongFunction<String> scoreFormatter = String::valueOf;
    private int targetWidth = 125;
    private boolean padding = false;
    private boolean trueCenter = true;

    public LeaderboardDisplay(
            @NotNull Viewable parent,
            @NotNull Supplier<LeaderboardData> globalLeaderboardSupplier,
            @NotNull Function<UUID, LeaderboardData.Entry> playerScoreSupplier,
            @NotNull Function<UUID, Component> displayNameSupplier
    ) {
        this(parent, globalLeaderboardSupplier, playerScoreSupplier, displayNameSupplier, 0, 0, 0, 1);
    }

    public LeaderboardDisplay(
            @NotNull Viewable parent,
            @NotNull Supplier<LeaderboardData> globalLeaderboardSupplier,
            @NotNull Function<UUID, LeaderboardData.Entry> playerScoreSupplier,
            @NotNull Function<UUID, Component> displayNameSupplier,
            double horizontalOffset, double screenAngle, double shift, double scale
    ) {
        this.parent = parent;
        this.globalLeaderboardSupplier = globalLeaderboardSupplier;
        this.playerScoreSupplier = playerScoreSupplier;
        this.displayNameSupplier = displayNameSupplier;

        initTextEntity(entriesEntity, horizontalOffset, TEXT_SCALE * scale, shift * scale, screenAngle);
        initTextEntity(titleEntity, horizontalOffset, TITLE_SCALE * scale, (shift + TITLE_SHIFT) * scale, screenAngle);
        initTextEntity(subtitleEntity, horizontalOffset, SUBTITLE_SCALE * scale, (shift + SUBTITLE_SHIFT) * scale, screenAngle);

        // Disable updates for this entities metadata. We will update the default text and it will still be
        // sent to new viewers as soon as they view the entity first. However, all subsequent updates will be
        // sent per player so we do not want to notify them when we change the default.
        entriesEntity.getEntityMeta().setNotifyAboutChanges(false);
        entriesEntity.getEntityMeta().setViewRange(100);
    }

    public @NotNull CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos pos) {
        return CompletableFuture.allOf(
                entriesEntity.setInstance(instance, pos),
                titleEntity.setInstance(instance, pos),
                subtitleEntity.setInstance(instance, pos),
                updatedEntity.setInstance(instance, pos)
        );
    }

    public @NotNull TextDisplayMeta entriesDisplay() {
        return entriesEntity.getEntityMeta();
    }

    public void editDisplays(@NotNull Consumer<TextDisplayMeta> editor) {
        editor.accept(entriesEntity.getEntityMeta());
        editor.accept(titleEntity.getEntityMeta());
        editor.accept(subtitleEntity.getEntityMeta());
        editor.accept(updatedEntity.getEntityMeta());
    }

    public void setScoreFormatter(@NotNull LongFunction<String> scoreFormatter) {
        this.scoreFormatter = scoreFormatter;
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public void setTrueCenter(boolean trueCenter) {
        this.trueCenter = trueCenter;
    }

    public void setTitle(@NotNull Component title) {
        setTitle(title, 0);
    }

    public void setSubtitle(@NotNull Component subtitle) {
        setSubtitle(subtitle, 0);
    }

    public void setTitle(@NotNull Component title, double shift) {
        titleEntity.getEntityMeta().setText(title);
        if (shift != 0) {
            titleEntity.getEntityMeta().setTranslation(titleEntity.getEntityMeta()
                    .getTranslation().add(0, shift, 0));
        }
    }

    public void setSubtitle(@NotNull Component subtitle, double shift) {
        subtitleEntity.getEntityMeta().setText(subtitle);
        if (shift != 0) {
            subtitleEntity.getEntityMeta().setTranslation(subtitleEntity.getEntityMeta()
                    .getTranslation().add(0, shift, 0));
        }
    }

    public void setUpdated(@NotNull Component updated) {
        updatedEntity.getEntityMeta().setText(updated);
    }

    public void setPadding(boolean padding) {
        this.padding = padding;
    }

    @Blocking
    public void update() {
        cachedData = globalLeaderboardSupplier.get();
        cachedTopTen = buildTop10(displayNameSupplier, cachedData);

        // Set the default text for new viewers while we fetch their data.
        var component = cachedTopTen
                .appendNewline().append(Component.text("You: —"));
        if (padding) component = component.appendNewline();
        entriesEntity.getEntityMeta().setText(component);

        // Update for all the viewers
        entriesEntity.getViewers().forEach(this::update);
    }

    @Blocking
    public void update(@NotNull Player player) {
        if (cachedData == null) return;

        var playerId = player.getUuid();
        var entry = cachedData.top().stream()
                .filter(e -> e.player().equals(playerId))
                .findFirst()
                .orElseGet(() -> playerScoreSupplier.apply(playerId));

        var content = cachedTopTen.appendNewline().append(
                Component.text("You: " + scoreFormatter.apply(entry == null ? 0 : entry.score())));
        if (entry != null && entry.rank() != -1) {
            content = content.append(Component.text(" (", NamedTextColor.GRAY)
                    .append(Component.text("#" + entry.rank()))
                    .append(Component.text(")", NamedTextColor.GRAY)));
        }
        if (padding) content = content.appendNewline();

        Map<Integer, Metadata.Entry<?>> metaUpdates = Map.of(MetadataDef.TextDisplay.TEXT.index(), Metadata.Component(content));
        player.sendPacket(new EntityMetaDataPacket(entriesEntity.getEntityId(), metaUpdates));
    }

    public static void initTextEntity(@NotNull LeaderboardDisplay.TextDisplay entity, double horizontalOffset, double scale, double shift, double screenAngle) {
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

    private @NotNull Component buildTop10(@NotNull Function<UUID, Component> nameFunc, @NotNull LeaderboardData data) {
        List<Component> names = new ArrayList<>();

        // Compute the target width of each line
        int maxWidth = 0;
        for (var entry : data.top()) {
            var name = LanguageProviderV2.translate(nameFunc.apply(entry.player()));
            names.add(name);
            maxWidth = Math.max(maxWidth, measureLine(name, entry.score(), entry.rank()));
        }

        // Rebuild each line properly with the known length
        var result = Component.text();
        if (padding) result.appendNewline().appendNewline().appendNewline();
        for (int i = 0; i < data.top().size(); i++) {
            var entry = data.top().get(i);
            result.append(buildLine(names.get(i), entry.score(), entry.rank(), targetWidth, trueCenter))
                    .appendNewline();
        }
        for (int i = data.top().size(); i < 10; i++) {
            result.append(buildLine(Component.text("—"),
                    0, i + 1, targetWidth, trueCenter)).appendNewline();
        }
        return result.build();
    }

    private int measureLine(@NotNull Component playerName, long score, int rank) {
        var plainName = PlainTextComponentSerializer.plainText().serialize(playerName);
        return FontUtil.measureText(String.format("#%d%s%s", trueCenter ? rank : 10, plainName, scoreFormatter.apply(score)));
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

        int lpDiff = FontUtil.measureText("#10") - FontUtil.measureText("#" + rank);
        var component = Component.text("#" + rank)
                .append(Component.text(FontUtil.computeOffset(leftPadding + (trueCenter ? 0 : lpDiff))))
                .append(playerName)
                .append(Component.text(FontUtil.computeOffset(padding - leftPadding)))
                .append(Component.text(scoreFormatter.apply(score)));
        if (!this.padding) {
            return component;
        }
        return Component.textOfChildren(HORIZONTAL_PADDING, component, HORIZONTAL_PADDING);
    }

    public static class TextDisplay extends Entity {

        public TextDisplay() {
            super(EntityType.TEXT_DISPLAY, UUID.randomUUID());

            hasPhysics = false;
            setNoGravity(true);
            collidesWithEntities = false;
        }

        @Override
        protected void movementTick() {
            // Intentionally do nothing
        }

        @Override
        public @NotNull TextDisplayMeta getEntityMeta() {
            return (TextDisplayMeta) super.getEntityMeta();
        }

        @Override
        public void updateNewViewer(@NotNull Player player) {
            super.updateNewViewer(player);

            AxiomPlayer.get(player).updateIgnoredEntities(it -> it.add(this.getUuid()));
        }

        @Override
        public void updateOldViewer(@NotNull Player player) {
            super.updateOldViewer(player);

            AxiomPlayer.get(player).updateIgnoredEntities(it -> it.remove(this.getUuid()));
        }
    }
}
