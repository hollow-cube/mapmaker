package net.hollowcube.mapmaker.map;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SaveState {
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

    SaveStateType.Serializer<?> serializer;
    Object state;

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

    public <T> @NotNull T state(@NotNull Class<T> stateType) {
        if (state == null)
            throw new IllegalStateException("State not loaded");
        if (!stateType.isAssignableFrom(state.getClass()))
            throw new IllegalArgumentException("State type mismatch. had " + state.getClass() + ", expected " + stateType);
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
                .setCompleted(completed);
        if (serializer != null && state != null) {
            req.setState(state, serializer);
        }
        return req;
    }


}
