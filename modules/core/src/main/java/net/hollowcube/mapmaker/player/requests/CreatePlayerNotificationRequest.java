package net.hollowcube.mapmaker.player.requests;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record CreatePlayerNotificationRequest(
    @NotNull String type, @NotNull String key, @Nullable JsonObject data, @Nullable Integer expiresIn
) {
}
