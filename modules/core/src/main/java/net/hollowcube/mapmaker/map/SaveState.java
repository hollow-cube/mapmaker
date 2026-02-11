package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@RuntimeGson
public class SaveState {
    private String id;
    private String playerId;
    private String mapId;
    private SaveStateType type;
    private boolean completed;
    private long playtime;
    private long ticks;
    private transient long playStartTime;
    int dataVersion;
    private int protocolVersion;

    SaveStateType.Serializer<?> serializer;
    Object state;

    public SaveState() {
    }

    public SaveState(@NotNull String id, @NotNull String playerId, @NotNull String mapId, @NotNull SaveStateType type, @NotNull SaveStateType.Serializer<?> serializer, @NotNull Object state) {
        this.id = id;
        this.playerId = playerId;
        this.mapId = mapId;
        this.type = type;

        this.serializer = serializer;
        this.state = state;
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
        setPlaytime(playtime + currentTime - playStartTime);
        playStartTime = currentTime;
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

    public @NotNull SaveStateUpdateRequest createUpdateRequest() {
        var req = new SaveStateUpdateRequest()
                .setPlaytime(playtime)
                .setTicks(ticks)
                .setCompleted(completed)
                .setProtocolVersion(protocolVersion);
        if (serializer != null && state != null) {
            req.setState(state, serializer);
        }
        return req;
    }

    public @NotNull SaveStateUpdateRequest createUpsertRequest() {
        var req = new SaveStateUpdateRequest()
                .setType(type)
                .setPlaytime(playtime)
                .setTicks(ticks)
                .setCompleted(completed)
                .setProtocolVersion(protocolVersion);
        if (serializer != null && state != null) {
            req.setState(state, serializer);
        }
        return req;
    }

    public SaveState copy(Object newState) {
        if (state.getClass() != newState.getClass())
            throw new UnsupportedOperationException("Cannot copy SaveState with different state type. Original state: " + state.getClass() + ", new state: " + newState.getClass());

        var copy = new SaveState();
        copy.id = id;
        copy.playerId = playerId;
        copy.mapId = mapId;
        copy.type = type;
        copy.completed = completed;
        copy.playtime = playtime;
        copy.ticks = ticks;
        copy.playStartTime = playStartTime;
        copy.dataVersion = dataVersion;
        copy.protocolVersion = protocolVersion;
        copy.serializer = serializer;
        copy.state = newState;

        // Snapshot the old state here.
        copy.updatePlaytime();
        copy.setPlayStartTime(0);
        return copy;
    }

}
