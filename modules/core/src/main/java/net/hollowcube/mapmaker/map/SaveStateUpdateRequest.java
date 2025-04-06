package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.NotNull;

public class SaveStateUpdateRequest {
    JsonObject updates = new JsonObject();

    public boolean hasChanges() {
        return !updates.isEmpty();
    }

    public JsonObject updates() {
        return updates;
    }

    public @NotNull SaveStateUpdateRequest setCompleted(boolean completed) {
        updates.addProperty("completed", completed);
        return this;
    }

    public @NotNull SaveStateUpdateRequest setPlaytime(long playtime) {
        updates.addProperty("playtime", playtime);
        return this;
    }

    public @NotNull SaveStateUpdateRequest setState(@NotNull Object state, @NotNull SaveStateType.Serializer<?> serializer) {
        updates.add(serializer.name(), ((Codec<Object>) serializer.codec()).encode(Transcoder.JSON, state).orElseThrow());
        return this;
    }

}
