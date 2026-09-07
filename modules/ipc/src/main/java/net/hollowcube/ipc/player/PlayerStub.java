package net.hollowcube.ipc.player;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.hollowcube.ipc.util.JsonValueAdapter;

import java.util.UUID;

/// Permissions use bit 0 for extended limits and bit 63 for staff.
public record PlayerStub(
    UUID id,
    String username,
    DisplayName displayName,
    long permissions,
    @JsonAdapter(JsonValueAdapter.class) JsonObject settings
) {}
