package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public class PlayerDataUpdateRequest {
    private String username = null;
    private List<String> ipHistory = null;
    private Instant lastOnline = null;

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

    public void setTfState(String tfState) {
        this.tfState = tfState;
    }

}
