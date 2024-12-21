package net.hollowcube.mapmaker.map.world.savestate;

import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PlayState {
    public static final int NO_RESET_HEIGHT = Integer.MIN_VALUE;

    private static final MapCodec<Map<Long, Block>> GHOST_BLOCKS_CODEC = Codec.unboundedMap(ExtraCodecs.LONG_STRING, ExtraCodecs.BLOCK_STATE_STRING)
            .optionalFieldOf("ghostBlocks", Map.of());
    public static Codec<PlayState> CODEC;

    static {
        CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.Lazy(() -> CODEC).optionalFieldOf("lastState").forGetter(PlayState::lastState),
                Codec.STRING.listOf().fieldOf("history").orElseGet(s -> {
                }, ArrayList::new).forGetter(PlayState::history),
                Codec.INT.optionalFieldOf("progressIndex").forGetter(PlayState::progressIndex),
                Codec.LONG.optionalFieldOf("timeLimit").forGetter(PlayState::timeLimit),
                Codec.INT.optionalFieldOf("resetHeight").forGetter(PlayState::resetHeight),
                PotionEffectList.NULL_MAPPED_CODEC.forGetter(PlayState::potionEffects),
                ExtraCodecs.POS.optionalFieldOf("pos").forGetter(PlayState::pos),
                Codec.INT.optionalFieldOf("maxLives").forGetter(PlayState::maxLives),
                Codec.INT.optionalFieldOf("lives").forGetter(PlayState::lives),
                GHOST_BLOCKS_CODEC.forGetter(PlayState::ghostBlocks),
                HotbarItems.CODEC.optionalFieldOf("items", HotbarItems.EMPTY).forGetter(PlayState::items)
        ).apply(i, PlayState::new));
    }

    public static final SaveStateType.Serializer<PlayState> SERIALIZER = new SaveStateType.Serializer<>() {
        @Override
        public @NotNull String name() {
            return "playState";
        }

        @Override
        public @NotNull Codec<PlayState> codec() {
            return CODEC;
        }

        @Override
        public @NotNull MCDataType dataType() {
            return HCTypeRegistry.PLAY_STATE;
        }
    };

    private Optional<PlayState> lastState; // The previous state of the player (ie at the last checkpoint)

    // Has two meanings:
    // in main state -> The list of status reached since the last checkpoint.
    // in last state -> The last entry is always the checkpoint that was reached at that state.
    private final List<String> history;
    private Optional<Integer> progressIndex;
    private Optional<Long> timeLimit; // Remaining time limit in ms
    private Optional<Integer> resetHeight;
    private PotionEffectList potionEffects;
    private Optional<Pos> pos;
    private Optional<Integer> maxLives; // Maximum number of lives for the current state
    private Optional<Integer> lives; // Number of lives remaining for the current state
    private Map<Long, Block> ghostBlocks;
    private HotbarItems items;

    private boolean tempReset = false;

    public PlayState() {
        this(Optional.empty(), List.of(), Optional.empty(),
                Optional.empty(), Optional.empty(), new PotionEffectList(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Map.of(), HotbarItems.EMPTY);
    }

    public PlayState(
            Optional<PlayState> lastState, List<String> statusEffects,
            Optional<Integer> progressIndex, Optional<Long> timeLimit,
            Optional<Integer> resetHeight,
            PotionEffectList potionEffects, Optional<Pos> pos,
            Optional<Integer> maxLives, Optional<Integer> lives,
            Map<Long, Block> ghostBlocks, HotbarItems items
    ) {
        this.lastState = lastState;
        this.history = new ArrayList<>(statusEffects);
        this.progressIndex = progressIndex;
        this.timeLimit = timeLimit;
        this.resetHeight = resetHeight;
        this.potionEffects = potionEffects;
        this.pos = pos;
        this.maxLives = maxLives;
        this.lives = lives;
        this.ghostBlocks = new HashMap<>(ghostBlocks);
        this.items = items;

        this.tempReset = true;
    }

    public Optional<PlayState> lastState() {
        return lastState;
    }

    public boolean hasStatus(@NotNull String id) {
        return history.contains(id);
    }

    public List<String> history() {
        return history;
    }

    public Optional<Integer> progressIndex() {
        return progressIndex;
    }

    public Optional<Long> timeLimit() {
        return timeLimit;
    }

    public Optional<Integer> resetHeight() {
        return resetHeight;
    }

    public PotionEffectList potionEffects() {
        return potionEffects;
    }

    public Optional<Pos> pos() {
        return pos;
    }

    public Optional<Integer> maxLives() {
        return maxLives;
    }

    public Optional<Integer> lives() {
        return lives;
    }

    public Map<Long, Block> ghostBlocks() {
        return ghostBlocks;
    }

    public void setGhostBlocks(Map<Long, Block> blockMap) {
        this.ghostBlocks = new HashMap<>(blockMap);
    }

    public void setLastState(@Nullable PlayState lastState) {
        this.lastState = Optional.ofNullable(lastState);
    }

    public void addStatus(@NotNull String id) {
        if (!history.contains(id)) history.add(id);
    }

    public void clearStatus() {
        history.clear();
    }

    public void setProgressIndex(int progressIndex) {
        this.progressIndex = progressIndex == -1 ? Optional.empty() : Optional.of(progressIndex);
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit <= 0 ? Optional.empty() : Optional.of(timeLimit);
    }

    public void setResetHeight(int resetHeight) {
        this.resetHeight = resetHeight == NO_RESET_HEIGHT ? Optional.empty() : Optional.of(resetHeight);
    }

    public void setPos(@Nullable Pos pos) {
        this.pos = Optional.ofNullable(pos);
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = maxLives <= 0 ? Optional.empty() : Optional.of(maxLives);
    }

    public void setLives(int lives) {
        this.lives = lives <= 0 ? Optional.empty() : Optional.of(lives);
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

    public @NotNull PlayState copy() {
        return new PlayState(
                lastState, history, progressIndex, timeLimit, resetHeight,
                potionEffects.copy(), pos, maxLives, lives, new HashMap<>(ghostBlocks),
                items
        );
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean includeLast) {
        return "PlayState{" +
                "lastState=" + lastState.isPresent() +
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

    private @NotNull String pretty(@NotNull Optional<?> optional) {
        return optional.map(Object::toString).orElse("null");
    }
}
