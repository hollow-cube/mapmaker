package net.hollowcube.mapmaker.map.world.savestate;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.map.feature.play.setting.SavedMapSettings;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public final class PlayState {
    public static final int NO_RESET_HEIGHT = Integer.MIN_VALUE;

    private static final StructCodec<Map<Long, Block>> GHOST_BLOCKS_CODEC = StructCodec.struct(
            "ghostBlocks", ExtraCodecs.LONG_STRING.mapValue(ExtraCodecs.BLOCK_STATE_STRING).optional(Map.of()), Function.identity(),
            it -> it);
    public static Codec<PlayState> CODEC = Codec.Recursive(codec -> StructCodec.struct(
            "lastState", codec.optional(), PlayState::lastState,
            "history", Codec.STRING.list().optional(List.of()), PlayState::history,
            "progressIndex", Codec.INT.optional(), PlayState::progressIndex,
            "timeLimit", Codec.LONG.optional(), PlayState::timeLimit,
            "resetHeight", Codec.INT.optional(), PlayState::resetHeight,
            StructCodec.INLINE, PotionEffectList.CODEC, PlayState::potionEffects,
            "pos", ExtraCodecs.POS.optional(), PlayState::pos,
            "maxLives", Codec.INT.optional(), PlayState::maxLives,
            "lives", Codec.INT.optional(), PlayState::lives,
            StructCodec.INLINE, GHOST_BLOCKS_CODEC.optional(), PlayState::ghostBlocks,
            "items", HotbarItems.CODEC.optional(HotbarItems.EMPTY), PlayState::items,
            "settings", SavedMapSettings.CODEC.optional(), PlayState::settings,
            PlayState::new));

    public static final SaveStateType.Serializer<PlayState> SERIALIZER = SaveStateType.serializer("playState", CODEC, HCDataTypes.PLAY_STATE);

    private PlayState lastState; // The previous state of the player (ie at the last checkpoint)

    // Has two meanings:
    // in main state -> The list of status reached since the last checkpoint.
    // in last state -> The last entry is always the checkpoint that was reached at that state.
    private final List<String> history;
    private Integer progressIndex;
    private Long timeLimit; // Remaining time limit in ms
    private Integer resetHeight;
    private PotionEffectList potionEffects;
    private Pos pos;
    private Integer maxLives; // Maximum number of lives for the current state
    private Integer lives; // Number of lives remaining for the current state
    private Map<Long, Block> ghostBlocks;
    private HotbarItems items;
    private final SavedMapSettings overridenSettings;

    private boolean tempReset = false;

    public PlayState() {
        this(null, null, null, null, null,
                null, null, null, null, null,
                null, null);
    }

    public PlayState(
            @Nullable PlayState lastState, @Nullable List<String> statusEffects,
            @Nullable Integer progressIndex, @Nullable Long timeLimit,
            @Nullable Integer resetHeight,
            @Nullable PotionEffectList potionEffects, @Nullable Pos pos,
            @Nullable Integer maxLives, @Nullable Integer lives,
            @Nullable Map<Long, Block> ghostBlocks, @Nullable HotbarItems items,
            @Nullable SavedMapSettings overridenSettings
    ) {
        this.lastState = lastState;
        this.history = new ArrayList<>(Objects.requireNonNullElse(statusEffects, List.of()));
        this.progressIndex = progressIndex;
        this.timeLimit = timeLimit;
        this.resetHeight = resetHeight;
        this.potionEffects = potionEffects;
        this.pos = pos;
        this.maxLives = maxLives;
        this.lives = lives;
        this.ghostBlocks = new HashMap<>(Objects.requireNonNullElse(ghostBlocks, Map.of()));
        this.items = Objects.requireNonNullElse(items, HotbarItems.EMPTY);
        this.overridenSettings = Objects.requireNonNullElseGet(overridenSettings, SavedMapSettings::new);

        this.tempReset = true;
    }

    public @Nullable PlayState lastState() {
        return lastState;
    }

    public boolean hasStatus(@NotNull String id) {
        return history.contains(id);
    }

    public @NotNull List<String> history() {
        return history;
    }

    public @Nullable Integer progressIndex() {
        return progressIndex;
    }

    public @Nullable Long timeLimit() {
        return timeLimit;
    }

    public @Nullable Integer resetHeight() {
        return resetHeight;
    }

    public @NotNull PotionEffectList potionEffects() {
        return potionEffects;
    }

    public @Nullable Pos pos() {
        return pos;
    }

    public @Nullable Integer maxLives() {
        return maxLives;
    }

    public @Nullable Integer lives() {
        return lives;
    }

    public @NotNull Map<Long, Block> ghostBlocks() {
        return ghostBlocks;
    }

    public void setGhostBlocks(@NotNull Map<Long, Block> blockMap) {
        this.ghostBlocks = new HashMap<>(blockMap);
    }

    public void setLastState(@Nullable PlayState lastState) {
        this.lastState = lastState;
    }

    public void addStatus(@NotNull String id) {
        if (!history.contains(id)) history.add(id);
    }

    public void clearStatus() {
        history.clear();
    }

    public void setProgressIndex(int progressIndex) {
        this.progressIndex = progressIndex == -1 ? null : progressIndex;
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit <= 0 ? null : timeLimit;
    }

    public void setResetHeight(int resetHeight) {
        this.resetHeight = resetHeight == NO_RESET_HEIGHT ? null : resetHeight;
    }

    public void setPos(@Nullable Pos pos) {
        this.pos = pos;
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = maxLives <= 0 ? null : maxLives;
    }

    public void setLives(int lives) {
        this.lives = lives <= 0 ? null : lives;
    }

    public boolean tempReset() {
        return tempReset;
    }

    public void setTempReset(boolean tempReset) {
        this.tempReset = tempReset;
    }

    public HotbarItems items() {
        return items;
    }

    public void setItems(HotbarItems items) {
        this.items = items;
    }

    public SavedMapSettings settings() {
        return overridenSettings;
    }

    public @NotNull PlayState copy() {
        return new PlayState(
                lastState, history, progressIndex, timeLimit, resetHeight,
                potionEffects.copy(), pos, maxLives, lives, new HashMap<>(ghostBlocks),
                items, overridenSettings.copy()
        );
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean includeLast) {
        return "PlayState{" +
                "lastState=" + String.valueOf(lastState != null) +
                ", statusEffects=" + (history.size() > 1 ? history.size() : history) +
                ", progressIndex=" + pretty(progressIndex) +
                ", timeLimit=" + pretty(timeLimit) +
                ", resetHeight=" + pretty(resetHeight) +
                ", potionEffects=" + potionEffects +
                ", pos=" + pretty(pos) +
                ", maxLives=" + pretty(maxLives) +
                ", lives=" + pretty(lives) +
                '}';
    }

    private @NotNull String pretty(@NotNull Object optional) {
        return String.valueOf(optional);
    }
}
