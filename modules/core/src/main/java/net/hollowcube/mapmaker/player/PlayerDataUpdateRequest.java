package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public class PlayerDataUpdateRequest {
    private String username = null;
    private List<String> ipHistory = null;
    private Instant lastOnline = null;

    private Integer unlockedMapSlots = null;
    private String[] mapSlots = null;
    private String lastPlayedMap = null;
    private String lastEditedMap = null;

    private String tfState = null; // base64 bytes

    public @NotNull PlayerDataUpdateRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public @NotNull PlayerDataUpdateRequest setIpHistory(List<String> ipHistory) {
        this.ipHistory = ipHistory;
        return this;
    }

    public @NotNull PlayerDataUpdateRequest setLastOnline(Instant lastOnline) {
        this.lastOnline = lastOnline;
        return this;
    }

    public @NotNull PlayerDataUpdateRequest setUnlockedMapSlots(Integer unlockedMapSlots) {
        this.unlockedMapSlots = unlockedMapSlots;
        return this;
    }

    public @NotNull PlayerDataUpdateRequest setMapSlots(String[] mapSlots) {
        this.mapSlots = mapSlots;
        return this;
    }

    public void setLastPlayedMap(@Nullable String lastPlayedMap) {
        // Kinda strange to use an empty string to indicate set to null but here we are
        this.lastPlayedMap = lastPlayedMap == null ? "" : lastPlayedMap;
    }

    public void setLastEditedMap(@Nullable String lastEditedMap) {
        // Kinda strange to use an empty string to indicate set to null but here we are
        this.lastEditedMap = lastEditedMap == null ? "" : lastEditedMap;
    }

    public void setTfState(String tfState) {
        this.tfState = tfState;
    }
}
