package net.hollowcube.mapmaker.util;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public record GenericServiceError(String code, String message, @Nullable JsonObject context) {
}
