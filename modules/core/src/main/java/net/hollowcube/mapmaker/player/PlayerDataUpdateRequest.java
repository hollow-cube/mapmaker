package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public class PlayerDataUpdateRequest {
    private String username = null;
    private List<String> ipHistory = null;
    private Instant lastOnline = null;
    private Integer unlockedMapSlots = null;
    private String[] mapSlots = null;

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

    public void setTfState(String tfState) {
        this.tfState = tfState;
    }
}
