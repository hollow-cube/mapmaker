package net.hollowcube.mapmaker.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.util.dfu.ExtraCodecs;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTList;
import org.jglrxavpok.hephaistos.nbt.NBTType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
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

    private transient SaveStateUpdateRequest updates = new SaveStateUpdateRequest();

    private String id;
    private String playerId;
    private String mapId;
    private SaveStateType type;
    private boolean completed;
    private long playtime;
    private transient long playStartTime;

    private PlayState playState = null;

    // Editing
    private Pos pos = null;
    private boolean isFlying = false;
    private String inventory = null; // base64 bytes

    public SaveState() {
    }

    public SaveState(@NotNull String id, @NotNull String playerId, @NotNull String mapId, @NotNull SaveStateType type) {
        this.id = id;
        this.playerId = playerId;
        this.mapId = mapId;
        this.type = type;
    }

    public @NotNull SaveStateUpdateRequest getUpdateRequest() {
        var updates = this.updates;
        this.updates = new SaveStateUpdateRequest();
        return updates;
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
        updates.setCompleted(completed);
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
        updates.setPlaytime(playtime);
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

    public @NotNull PlayState playState() {
        if (playState == null) playState = new PlayState();
        return playState;
    }

    public void setPlayState(@NotNull PlayState playState) {
        this.playState = playState;
    }

    // OLD STUFF THAT NEEDS TO BE DELETED PROBABLY BELOW

    public @Nullable Pos pos() {
        return pos;
    }

    public void setPos(Pos pos) {
        updates.setPos(pos);
        this.pos = pos;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public void setFlying(boolean flying) {
        updates.setFlying(flying);
        isFlying = flying;
    }

    public byte @Nullable [] inventory() {
        if (inventory == null) return null;
        return Base64.getDecoder().decode(inventory);
    }

    // Utilities

    @SuppressWarnings("UnstableApiUsage")
    public @Nullable List<@NotNull ItemStack> getInventoryItems() {
        var inventory = inventory();
        if (inventory == null) return null;
        try {
            var compound = (NBTCompound) new NetworkBuffer(ByteBuffer.wrap(inventory)).read(NetworkBuffer.NBT);
            var items = compound.<NBTCompound>getList("Items");
            if (items == null) return null;
            return items.asListView().stream().map(ItemStack::fromItemNBT).toList();
        } catch (Exception e) {
            try {
                logger.warn("Failed to read modern inventory, trying legacy: {}", e.getMessage());
                return new NetworkBuffer(ByteBuffer.wrap(inventory))
                        .readCollection(NetworkBuffer.ITEM);
            } catch (Exception e2) {
                MinecraftServer.getExceptionManager().handleException(e2);
                return null;
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void setInventoryItems(@Nullable List<ItemStack> items) {
        if (items == null) {
            this.inventory = null;
        } else {
            var entries = new ArrayList<NBTCompound>();
            for (var item : items) entries.add(item.toItemNBT());
            var inventoryTag = new NBTCompound(Map.of(
                    "Items", new NBTList<>(NBTType.TAG_Compound, entries)
                    // Space left for other tags
            ));
            this.inventory = Base64.getEncoder().encodeToString(NetworkBuffer.makeArray(b -> b.write(NetworkBuffer.NBT, inventoryTag)));
        }
        updates.setInventory(this.inventory);
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
                    Codec.unboundedMap(ExtraCodecs.POTION_EFFECT, Codec.INT).optionalFieldOf("potionEffects", Map.of()).forGetter(PlayState::potionEffects),
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
        private Map<PotionEffect, Integer> potionEffects;
        private Optional<Pos> pos;
        private Optional<Integer> maxLives; // Maximum number of lives for the current state
        private Optional<Integer> lives; // Number of lives remaining for the current state

        public PlayState() {
            this(Optional.empty(), List.of(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Map.of(), Optional.empty(),
                    Optional.empty(), Optional.empty());
        }

        public PlayState(
                Optional<PlayState> lastState, List<String> statusEffects,
                Optional<Integer> progressIndex, Optional<Long> timeLimit,
                Optional<Integer> resetHeight,
                Map<PotionEffect, Integer> potionEffects, Optional<Pos> pos,
                Optional<Integer> maxLives, Optional<Integer> lives
        ) {
            this.lastState = lastState;
            this.history = new ArrayList<>(statusEffects);
            this.progressIndex = progressIndex;
            this.timeLimit = timeLimit;
            this.resetHeight = resetHeight;
            this.potionEffects = new HashMap<>(potionEffects);
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

        public Map<PotionEffect, Integer> potionEffects() {
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

        public void setPotionEffects(Map<PotionEffect, Integer> potionEffects) {
            this.potionEffects = potionEffects;
        }

        public void setPos(Optional<Pos> pos) {
            this.pos = pos;
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
                    potionEffects, pos, maxLives, lives
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
