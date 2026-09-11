package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.datafix.DataType;
import net.hollowcube.ipc.map.SaveStateData;
import net.hollowcube.ipc.map.SaveStateType;
import net.hollowcube.ipc.map.SaveStateUpdate;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/// A run or edit session in progress on this server, which [SaveStateData] is only the stored
/// shape of: this one ticks, accrues playtime as the player plays, carries the decoded state
/// object and the serializer that wrote it, and inherits the attempt counters of the save state a
/// hard reset replaced.
public class SaveState {

    /// How a map server turns its state into the json the api stores, and back.
    public interface Serializer<T> {
        @NotNull Codec<T> codec();

        @NotNull DataType dataType();
    }

    public static <T> Serializer<T> serializer(Codec<T> codec, DataType dataType) {
        record SerializerImpl<T>(Codec<T> codec, DataType dataType) implements Serializer<T> {
        }
        return new SerializerImpl<>(codec, dataType);
    }

    private final String id;
    private final String playerId;
    private final String mapId;
    private final SaveStateType type;
    private final Instant created;
    private boolean completed;
    private long playtime;
    private long ticks;
    // Carried across this player's save state lineage on the map: a hard reset starts a fresh
    // save state (and the old one is often never written), so the newest one holds the aggregate.
    private int resets;
    private long totalPlaytime; // never zeroed, unlike playtime on only-sprint maps
    private long playStartTime;
    private int dataVersion;
    private int protocolVersion;

    private Double score;

    // TODO: nothing here should be public in future.
    public Serializer<?> serializer;
    public Object state;

    /// A state made on this server, reaching the api with its first save.
    public SaveState(@NotNull String id, @NotNull String playerId, @NotNull String mapId, @NotNull SaveStateType type, @NotNull Serializer<?> serializer, @NotNull Object state) {
        this.id = id;
        this.playerId = playerId;
        this.mapId = mapId;
        this.type = type;
        this.created = Instant.now();
        this.dataVersion = DataFixer.maxVersion();

        this.serializer = serializer;
        this.state = state;
    }

    /// A state read back from the api; `state` is what `serializer` decoded at `dataVersion`,
    /// or null when the caller only wanted the numbers.
    public SaveState(@NotNull SaveStateData data, int dataVersion, @Nullable Serializer<?> serializer, @Nullable Object state) {
        this.id = data.id().toString();
        this.playerId = data.playerId().toString();
        this.mapId = data.mapId().toString();
        this.type = data.type();
        this.created = data.created();
        this.completed = data.completed();
        this.playtime = data.playtime();
        this.ticks = data.ticks();
        this.resets = data.resets();
        this.totalPlaytime = data.totalPlaytime();
        this.dataVersion = dataVersion;
        this.protocolVersion = data.protocolVersion();
        this.score = data.score();
        this.serializer = serializer;
        this.state = state;
    }

    private SaveState(SaveState copy, Object newState) {
        this.id = copy.id;
        this.playerId = copy.playerId;
        this.mapId = copy.mapId;
        this.type = copy.type;
        this.created = copy.created;
        this.completed = copy.completed;
        this.playtime = copy.playtime;
        this.ticks = copy.ticks;
        this.resets = copy.resets;
        this.totalPlaytime = copy.totalPlaytime;
        this.playStartTime = copy.playStartTime;
        this.dataVersion = copy.dataVersion;
        this.protocolVersion = copy.protocolVersion;
        this.score = copy.score;
        this.serializer = copy.serializer;
        this.state = newState;
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

    public int dataVersion() {
        return dataVersion;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void uncomplete() {
        this.completed = false;
    }

    public void complete(long time) {
        this.completed = true;
        updatePlaytime(time);
        playStartTime = 0;
    }

    public long getPlaytime() {
        return playtime;
    }

    public long getTicks() {
        return this.ticks;
    }

    public long getEffectivePlaytime() {
        return Math.max(playtime, ticks * 50);
    }

    /// Hard resets that preceded this save state for this player on this map.
    public int getResets() {
        return resets;
    }

    /// Active playtime across this save state and every one before it in the lineage, at the last save.
    /// States written before this existed report zero, so the current playtime is the floor.
    public long getTotalPlaytime() {
        return Math.max(totalPlaytime, playtime);
    }

    /// {@link #getTotalPlaytime()} to the millisecond at this moment.
    public long getRealTotalPlaytime() {
        return getPlayStartTime() != 0 ? getTotalPlaytime() + System.nanoTime() / 1_000_000 - getPlayStartTime() : getTotalPlaytime();
    }

    /// Seeds the lineage aggregate from the save state this one replaces on a hard reset.
    public void inheritAttemptStats(@NotNull SaveState previous) {
        this.resets = previous.resets + 1;
        this.totalPlaytime = previous.getRealTotalPlaytime();
    }

    /**
     * Returns the current playtime to the millisecond at this moment, as opposed to {@link #getPlaytime()}
     * which returns the playtime at the last save.
     */
    public long getRealPlaytime() {
        return getPlayStartTime() != 0 ? getPlaytime() + System.nanoTime() / 1_000_000 - getPlayStartTime() : getPlaytime();
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

    public void tick() {
        if (playStartTime == 0) return;
        ticks++;
    }

    public void updatePlaytime() {
        updatePlaytime(System.nanoTime() / 1_000_000);
    }

    public void updatePlaytime(long currentTime) {
        if (playStartTime == 0) return;
        var elapsed = currentTime - playStartTime;
        setPlaytime(playtime + elapsed);
        totalPlaytime = getTotalPlaytime() + elapsed;
        playStartTime = currentTime;
    }

    public double getScore() {
        return OpUtils.or(score, () -> (double) getEffectivePlaytime());
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public <T> @NotNull T state(@NotNull Class<T> stateType) {
        if (state == null)
            throw new IllegalStateException("State not loaded");
        if (!stateType.isAssignableFrom(state.getClass()))
            throw new IllegalArgumentException("State type mismatch. had " + state.getClass() + ", expected " + stateType + " details: " + Map.of(
                "id", id,
                "playerId", playerId,
                "mapId", mapId,
                "state", state,
                "stateType", stateType,
                "serializer", serializer,
                "dataVersion", dataVersion,
                "playtime", playtime,
                "ticks", ticks,
                "completed", completed
            ));
        return stateType.cast(state);
    }

    public <T> @Nullable T tryGetState(@NotNull Class<T> stateType) {
        if (state == null) return null;
        if (!stateType.isAssignableFrom(state.getClass())) return null;
        return stateType.cast(state);
    }

    public void setState(@NotNull Object state) {
        if (!this.state.getClass().isAssignableFrom(state.getClass()))
            throw new IllegalArgumentException("State type mismatch. had " + this.state.getClass() + ", expected " + state.getClass());
        this.state = state;
    }

    /// Everything the api needs to store this state as it is now. The state itself is encoded at
    /// the current data version, whatever version it was read at.
    @SuppressWarnings("unchecked")
    public @NotNull SaveStateUpdate createUpsertRequest() {
        var update = SaveStateUpdate.builder(type)
            .created(created)
            .playtime(playtime)
            .ticks(ticks)
            .attempts(resets, getTotalPlaytime())
            .dataVersion(DataFixer.maxVersion())
            .protocolVersion(protocolVersion)
            .completed(completed, score);
        if (serializer != null && state != null) {
            var coder = new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process());
            var encoded = ((Codec<Object>) serializer.codec()).encode(coder, state).orElseThrow();
            update.state(encoded instanceof JsonObject object ? object : new JsonObject());
        }
        return update.build();
    }

    public SaveState copy(Object newState) {
        if (state.getClass() != newState.getClass())
            throw new UnsupportedOperationException("Cannot copy SaveState with different state type. Original state: " + state.getClass() + ", new state: " + newState.getClass());

        var copy = new SaveState(this, newState);

        // Snapshot the old state here.
        copy.updatePlaytime();
        copy.setPlayStartTime(0);
        return copy;
    }

}
