package net.hollowcube.mapmaker.player.requests;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record CreatePlayerNotificationRequest(
    String type,
    String key,
    @Nullable JsonObject data,
    @Nullable Integer expiresIn
) {
}
