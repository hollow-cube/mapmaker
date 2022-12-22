package net.hollowcube.mapmaker.model;

import net.hollowcube.mapmaker.util.ExtraTags;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.tag.Tag;

import java.time.Instant;

/**
 * A player may have one or more save state for each map they have played.
 *
 * While playing a map, the
 */
public class SaveState {
    public static final Tag<SaveState> TAG = ExtraTags.Transient("mapmaker:map/save_state");

    private String id;
    private String playerId;
    private String mapId;
    private boolean completed = false;

    // The time this save state was created.
    private Instant startTime;
    // The total time spent inside this save state.
    private long playtime;

    private Pos pos;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public long getPlaytime() {
        return playtime;
    }

    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Pos getPos() {
        return pos;
    }

    public void setPos(Pos pos) {
        this.pos = pos;
    }

}
