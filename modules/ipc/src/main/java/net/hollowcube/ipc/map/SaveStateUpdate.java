package net.hollowcube.ipc.map;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.hollowcube.ipc.util.JsonValueAdapter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/// Everything a server writes back about a save state. The id is the caller's: a state is made on
/// the server that first needs it and reaches the database with its first save.
///
/// @param created       when the server made the state, for the insert that first sees it; null
///                      leaves the api to guess from `playtime`
/// @param resets        hard resets before this state, carried along the player's lineage on the map
/// @param totalPlaytime playtime across the lineage, never zeroed
/// @param state         null keeps whatever is stored
/// @param score         only read when `completed`; null scores the run by playtime
public record SaveStateUpdate(
    SaveStateType type,
    @Nullable Instant created,
    long playtime,
    long ticks,
    int resets,
    long totalPlaytime,
    int dataVersion,
    int protocolVersion,
    @JsonAdapter(JsonValueAdapter.class) @Nullable JsonObject state,
    boolean completed,
    @Nullable Double score
) {

    public static Builder builder(SaveStateType type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final SaveStateType type;
        private @Nullable Instant created;
        private long playtime;
        private long ticks;
        private int resets;
        private long totalPlaytime;
        private int dataVersion;
        private int protocolVersion;
        private @Nullable JsonObject state;
        private boolean completed;
        private @Nullable Double score;

        private Builder(SaveStateType type) {
            this.type = type;
        }

        public Builder created(@Nullable Instant created) {
            this.created = created;
            return this;
        }

        public Builder playtime(long playtime) {
            this.playtime = playtime;
            return this;
        }

        public Builder ticks(long ticks) {
            this.ticks = ticks;
            return this;
        }

        public Builder attempts(int resets, long totalPlaytime) {
            this.resets = resets;
            this.totalPlaytime = totalPlaytime;
            return this;
        }

        public Builder dataVersion(int dataVersion) {
            this.dataVersion = dataVersion;
            return this;
        }

        public Builder protocolVersion(int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder state(@Nullable JsonObject state) {
            this.state = state;
            return this;
        }

        public Builder completed(boolean completed, @Nullable Double score) {
            this.completed = completed;
            this.score = score;
            return this;
        }

        public SaveStateUpdate build() {
            return new SaveStateUpdate(
                type,
                created,
                playtime,
                ticks,
                resets,
                totalPlaytime,
                dataVersion,
                protocolVersion,
                state,
                completed,
                score
            );
        }
    }
}
