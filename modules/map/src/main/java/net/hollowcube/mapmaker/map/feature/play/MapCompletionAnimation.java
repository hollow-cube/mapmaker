package net.hollowcube.mapmaker.map.feature.play;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.player.AppliedRewards;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.function.Supplier;

public final class MapCompletionAnimation implements Supplier<TaskSchedule> {
    private static final Tag<MapCompletionAnimation> TAG = Tag.Transient("map_completion_animation");

    public static void schedule(@NotNull Player target, @NotNull AppliedRewards.Inventory rewards, @Nullable Runnable onComplete) {
        cancel(target);

        var animation = new MapCompletionAnimation(target, rewards, onComplete);
        target.scheduler().submitTask(animation);
        target.setTag(TAG, animation);
    }

    public static void cancel(@NotNull Player target) {
        var existing = target.getTag(TAG);
        if (existing != null) existing.cancel();
    }

    private static final BadSprite COINS_SPRITE = BadSprite.require("icon/store/oo_coins");
    private static final BadSprite CUBITS_SPRITE = BadSprite.require("icon/store/oo_cubits");
    private static final BadSprite EXP_SPRITE = BadSprite.require("icon/store/oo_exp");

    private static final Component[] TITLE_FRAMES = new Component[16 + 8];
    private static final int BACKGROUND_BORDER = 8;
    private static final BadSprite[] BACKGROUND = new BadSprite[8];
    private static final BadSprite BACKGROUND_LEFT = BadSprite.require("map_completion/black/left");
    private static final BadSprite BACKGROUND_RIGHT = BadSprite.require("map_completion/black/right");

    static {
        // First the frames from 0 to 15, then the 'undo' frames from 7 to 0.
        for (int i = 0; i < 16; i++) {
            var sprite = BadSprite.SPRITE_MAP.get("map_completion/anim/frame" + i);
            var builder = new FontUIBuilder();
            var mark = builder.mark();
            builder.offset(-sprite.width() / 2).drawInPlace(sprite);
            builder.restore(mark);
            builder.pos(0);
            TITLE_FRAMES[i] = builder.build(true);
        }
        for (int i = 0; i <= 7; i++)
            TITLE_FRAMES[16 + i] = TITLE_FRAMES[7 - i];
        for (int i = 0; i < 8; i++) {
            BACKGROUND[i] = BadSprite.SPRITE_MAP.get("map_completion/black/" + (1 << i));
        }
    }

    private final Player player;
    private final AppliedRewards.Inventory rewards;
    private final Runnable onComplete;

    private final Component subtitle;
    private int frame = 0;

    private boolean isCanceled = false;

    private MapCompletionAnimation(@NotNull Player player, @NotNull AppliedRewards.Inventory rewards, @Nullable Runnable onComplete) {
        this.player = player;
        this.rewards = rewards;
        this.onComplete = onComplete;

        this.subtitle = buildSubtitle();
    }

    @Override
    public TaskSchedule get() {
        if (isCanceled) return TaskSchedule.stop();
        if (frame == TITLE_FRAMES.length) {
            player.clearTitle();
            player.scheduleNextTick(_ -> onComplete.run());
            return TaskSchedule.stop();
        }

        player.showTitle(Title.title(
                TITLE_FRAMES[frame++], Component.empty(), //subtitle, todo: reenable when adding materials back
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
        ));

        if (frame == 16) {
            return TaskSchedule.tick(10);
        }

        return TaskSchedule.tick(2);
    }

    private void cancel() {
        this.isCanceled = true;
        player.clearTitle(); // Clear the title, will be replaced by new animation potentially.
    }

    private @NotNull Component buildSubtitle() {
        if (!rewards.hasExp() && !rewards.hasCoins() && !rewards.hasCubits() && !rewards.hasBackpack())
            return Component.empty();

        var builder = new FontUIBuilder();
        var mark = builder.mark();
        builder.pushColor(FontUtil.NO_SHADOW);

        // This function is a little confusing but basically we need to know the entire width before we start drawing,
        // so we figure out the texts and lengths first, then draw background and foreground.

        // === BEGIN TEXT LENGTH CALC ===
        int totalWidth = 0;
        String expText = null, coinsText = null, cubitsText = null, backpackText = null;
        int expTextLength = 0, coinsTextLength = 0, cubitsTextLength = 0, backpackTextLength = 0;

        if (rewards.hasExp()) {
            expText = "+" + rewards.exp();
            expTextLength = FontUtil.measureText(expText);
            totalWidth += expTextLength + 2 + EXP_SPRITE.width() + 2;

            if (rewards.hasCoins() || rewards.hasCubits() || rewards.hasBackpack())
                totalWidth += 5;
        }

        if (rewards.hasCoins()) {
            coinsText = "+" + rewards.coins();
            coinsTextLength = FontUtil.measureText(coinsText);
            totalWidth += coinsTextLength + 2 + COINS_SPRITE.width() + 2;

            if (rewards.hasCubits() || rewards.hasBackpack())
                totalWidth += 5;
        }

        if (rewards.hasCubits()) {
            cubitsText = "+" + rewards.cubits();
            cubitsTextLength = FontUtil.measureText(cubitsText);
            totalWidth += cubitsTextLength + 2 + CUBITS_SPRITE.width() + 2;

            if (rewards.hasBackpack())
                totalWidth += 5;
        }

        if (rewards.hasBackpack()) {
            var item = rewards.getItem();
            backpackText = "+1";
            backpackTextLength = FontUtil.measureText(backpackText);
            totalWidth += backpackTextLength + 2 + item.iconSprite().width() + 2;
        }

        // === BEGIN DRAWING ===

        builder.offset(-(totalWidth + BACKGROUND_BORDER) / 2);
        builder.append(buildBackground(totalWidth), totalWidth + BACKGROUND_BORDER);
        builder.offset(-(totalWidth + (BACKGROUND_BORDER / 2)));

        if (rewards.hasExp()) {
            builder.append(expText, expTextLength);
            builder.offset(2);
            builder.drawInPlace(EXP_SPRITE);

            if (rewards.hasCoins() || rewards.hasCubits() || rewards.hasBackpack())
                builder.offset(5);
        }

        if (rewards.hasCoins()) {
            builder.append(coinsText, coinsTextLength);
            builder.offset(2);
            builder.drawInPlace(COINS_SPRITE);

            if (rewards.hasCubits() || rewards.hasBackpack())
                builder.offset(5);
        }

        if (rewards.hasCubits()) {
            builder.append(cubitsText, cubitsTextLength);
            builder.offset(2);
            builder.drawInPlace(CUBITS_SPRITE);

            if (rewards.hasBackpack())
                builder.offset(5);
        }

        if (rewards.hasBackpack()) {
            var item = rewards.getItem();
            builder.append(backpackText, backpackTextLength);
            builder.offset(2);
            builder.drawInPlace(item.iconSprite());
        }

        builder.popColor();
        builder.restore(mark);
        builder.pos(0);
        return builder.build();
    }

    private @NotNull String buildBackground(int contentWidth) {
        var sb = new StringBuilder();
        Check.argCondition(contentWidth > 0b11111111, "Oof too big (round 2)!");

        sb.append(BACKGROUND_LEFT.fontChar()).append(FontUtil.computeOffset(-1));
        for (int i = 0; i < BACKGROUND.length; i++) {
            if ((contentWidth & (1 << i)) != 0) {
                sb.append(BACKGROUND[i].fontChar()).append(FontUtil.computeOffset(-1));
            }
        }
        sb.append(BACKGROUND_RIGHT.fontChar());

        return sb.toString();
    }
}
