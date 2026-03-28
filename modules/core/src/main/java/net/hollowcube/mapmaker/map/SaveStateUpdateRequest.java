package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class SaveStateUpdateRequest {
    JsonObject updates = new JsonObject();

    public boolean hasChanges() {
        return !updates.isEmpty();
    }

    public JsonObject updates() {
        return updates;
    }

    public @NotNull SaveStateUpdateRequest setType(@NotNull SaveStateType type) {
        updates.addProperty("type", type.name().toLowerCase(Locale.ROOT));
        return this;
    }

    public @NotNull SaveStateUpdateRequest setCompleted(boolean completed) {
        updates.addProperty("completed", completed);
        return this;
    }

    public @NotNull SaveStateUpdateRequest setPlaytime(long playtime) {
        updates.addProperty("playtime", playtime);
        return this;
    }

    public @NotNull SaveStateUpdateRequest setTicks(long playtime) {
        updates.addProperty("ticks", playtime);
        return this;
    }

    public @NotNull SaveStateUpdateRequest setProtocolVersion(int protocolVersion) {
        updates.addProperty("protocolVersion", protocolVersion);
        return this;
    }

    public @NotNull SaveStateUpdateRequest setLatency(Double start, Double end) {
        if (start == null || end == null || start < 0 || end < 0) return this;
        updates.addProperty("startLatency", start);
        updates.addProperty("endLatency", end);
        return this;
    }

    public @NotNull SaveStateUpdateRequest setState(@NotNull Object state, @NotNull SaveStateType.Serializer<?> serializer) {
        var coder = new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process());
        updates.add(serializer.name(), ((Codec<Object>) serializer.codec()).encode(coder, state).orElseThrow());
        return this;
    }

    public @NotNull SaveStateUpdateRequest setScore(Double score) {
        updates.addProperty("score", score);
        return this;
    }

}
