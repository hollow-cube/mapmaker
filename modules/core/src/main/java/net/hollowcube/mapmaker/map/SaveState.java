package net.hollowcube.mapmaker.map;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.util.dfu.ExtraCodecs;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SaveState {
    private static final Logger logger = LoggerFactory.getLogger(SaveState.class);

    public static final Tag<SaveState> TAG = Tag.Transient("mapmaker:map/save_state");

    public static @NotNull SaveState fromPlayer(@NotNull Player player) {
        return Objects.requireNonNull(optionalFromPlayer(player));
    }

    public static @Nullable SaveState optionalFromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private String id;
    private String playerId;
    private String mapId;
    private SaveStateType type;
    private boolean completed;
    private long playtime;
    private transient long playStartTime;

    @SerializedName("editState")
    private BuildState buildState = null;
    private PlayState playState = null;

    public SaveState() {
    }

    public SaveState(@NotNull String id, @NotNull String playerId, @NotNull String mapId, @NotNull SaveStateType type) {
        this.id = id;
        this.playerId = playerId;
        this.mapId = mapId;
        this.type = type;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String playerId() {
        return playerId;
    }

    public @NotNull String mapId() {
        return mapId;
    }

    public @NotNull SaveStateType type() {
        return type;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;

        // If completing, update the playtime for the final time.
        if (completed) {
            updatePlaytime();
            playStartTime = 0;
        }
    }

    public long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }

    public long getPlayStartTime() {
        return playStartTime;
    }

    public void setPlayStartTime(long playStartTime) {
        this.playStartTime = playStartTime;
    }

    public void updatePlaytime() {
        if (playStartTime == 0) return;
        setPlaytime(playtime + System.currentTimeMillis() - playStartTime);
        playStartTime = System.currentTimeMillis();
    }

    public @NotNull BuildState buildState() {
        Check.stateCondition(type != SaveStateType.EDITING, "Cannot access build state in non-editing save state");
        if (buildState == null) buildState = new BuildState();
        return buildState;
    }

    public @NotNull PlayState playState() {
        Check.stateCondition(type != SaveStateType.PLAYING && type != SaveStateType.VERIFYING, "Cannot access play state in non-playing save state");
        if (playState == null) playState = new PlayState();
        return playState;
    }

    public void setPlayState(@NotNull PlayState playState) {
        this.playState = playState;
    }

    public @NotNull SaveStateUpdateRequest createUpdateRequest() {
        var req = new SaveStateUpdateRequest()
                .setPlaytime(playtime)
                .setCompleted(completed);
        if (buildState != null) req.setBuildState(buildState);
        if (playState != null) req.setPlayState(playState);
        return req;
    }

    public static class BuildState {
        public static final Codec<BuildState> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.POS.optionalFieldOf("pos").forGetter(BuildState::pos),
                Codec.BOOL.optionalFieldOf("isFlying", false).forGetter(BuildState::isFlying),
                ExtraCodecs.ITEM_STACK_MAP_AS_BASE64.optionalFieldOf("inventory", Map.of()).forGetter(BuildState::inventory),
                Codec.INT.optionalFieldOf("selectedSlot", 0).forGetter(BuildState::selectedSlot)
        ).apply(i, BuildState::new));

        private Optional<Pos> pos;
        private boolean isFlying;
        private Map<Integer, ItemStack> inventory;
        private int selectedSlot;

        //todo for adding new build state we could have some kind of "build state contributor" which can supply new codecs which are dispatch-ed to their correct type.
        // maybe.

        public BuildState() {
            this(Optional.empty(), false, Map.of(), 0);
        }

        public BuildState(Optional<Pos> pos, boolean isFlying, Map<Integer, ItemStack> inventory, int selectedSlot) {
            this.pos = pos;
            this.isFlying = isFlying;
            this.inventory = inventory;
            this.selectedSlot = selectedSlot;
        }

        public @NotNull Optional<Pos> pos() {
            return pos;
        }

        public boolean isFlying() {
            return isFlying;
        }

        public @NotNull Map<Integer, ItemStack> inventory() {
            return inventory;
        }

        public int selectedSlot() {
            return selectedSlot;
        }

        public void setPos(@Nullable Pos pos) {
            this.pos = Optional.ofNullable(pos);
        }

        public void setFlying(boolean flying) {
            isFlying = flying;
        }

        public void setInventory(@NotNull Map<Integer, ItemStack> inventory) {
            this.inventory = inventory;
        }

        public void setSelectedSlot(int selectedSlot) {
            this.selectedSlot = selectedSlot;
        }
    }

    public static class PlayState {
        public static final int NO_RESET_HEIGHT = Integer.MIN_VALUE;

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
                    Codec.INT.optionalFieldOf("lives").forGetter(PlayState::lives)
            ).apply(i, PlayState::new));
        }

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

        public PlayState() {
            this(Optional.empty(), List.of(), Optional.empty(),
                    Optional.empty(), Optional.empty(), new PotionEffectList(),
                    Optional.empty(), Optional.empty(), Optional.empty());
        }

        public PlayState(
                Optional<PlayState> lastState, List<String> statusEffects,
                Optional<Integer> progressIndex, Optional<Long> timeLimit,
                Optional<Integer> resetHeight,
                PotionEffectList potionEffects, Optional<Pos> pos,
                Optional<Integer> maxLives, Optional<Integer> lives
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

        public @NotNull PlayState copy() {
            return new PlayState(
                    lastState, history, progressIndex, timeLimit, resetHeight,
                    potionEffects.copy(), pos, maxLives, lives
            );
        }

        @Override
        public String toString() {
            return toString(true);
        }

        public String toString(boolean includeLast) {
            return "PlayState{" +
                    "lastState=" + lastState.isPresent() +
                    ", statusEffects=" + history +
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

}
