package net.hollowcube.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static @NotNull String toPrettyJson(@NotNull JsonElement element) {
        return GSON.toJson(element);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static <T> @Nullable String toPrettyJson(@NotNull Codec<T> codec, @NotNull T value) {
        JsonElement jsonElement = codec.encode(new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process()), value).orElse(null);
        return jsonElement != null ? toPrettyJson(jsonElement) : null;
    }
}
