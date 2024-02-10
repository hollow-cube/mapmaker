package net.hollowcube.mapmaker.util;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record GenericServiceError(@NotNull String code, @NotNull String message, @Nullable JsonObject context) {
}
