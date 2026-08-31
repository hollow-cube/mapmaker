package net.hollowcube.mapmaker.util;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record GenericServiceError(@NotNull String code, @NotNull String message, @Nullable JsonObject context) {
}
