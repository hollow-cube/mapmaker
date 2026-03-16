package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.RegistryTranscoder;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class SaveStateUpdateRequest {
    JsonObject updates = new JsonObject();

    public boolean hasChanges() {
        return !updates.isEmpty();
    }

    public JsonObject updates() {
        return updates;
    }

    public SaveStateUpdateRequest setType(SaveStateType type) {
        updates.addProperty("type", type.name().toLowerCase(Locale.ROOT));
        return this;
    }

    public SaveStateUpdateRequest setCompleted(boolean completed) {
        updates.addProperty("completed", completed);
        return this;
    }

    public SaveStateUpdateRequest setPlaytime(long playtime) {
        updates.addProperty("playtime", playtime);
        return this;
    }

    public SaveStateUpdateRequest setTicks(long playtime) {
        updates.addProperty("ticks", playtime);
        return this;
    }

    public SaveStateUpdateRequest setProtocolVersion(int protocolVersion) {
        updates.addProperty("protocolVersion", protocolVersion);
        return this;
    }

    public SaveStateUpdateRequest setLatency(@Nullable Double start, @Nullable Double end) {
        if (start == null || end == null || start < 0 || end < 0) return this;
        updates.addProperty("startLatency", start);
        updates.addProperty("endLatency", end);
        return this;
    }

    public SaveStateUpdateRequest setState(Object state, SaveStateType.Serializer<?> serializer) {
        var coder = new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process());
        @SuppressWarnings("unchecked")
        var codec = (Codec<Object>) serializer.codec();
        updates.add(serializer.name(), codec.encode(coder, state).orElseThrow());
        return this;
    }

}
