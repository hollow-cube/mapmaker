package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.hollowcube.mapmaker.util.dfu.DFU;
import org.jetbrains.annotations.NotNull;

public class SaveStateUpdateRequest {
    JsonObject updates = new JsonObject();

    public boolean hasChanges() {
        return !updates.isEmpty();
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
        updates.add(serializer.name(), DFU.unwrap(((Codec<Object>) serializer.codec()).encodeStart(JsonOps.INSTANCE, state)));
        return this;
    }

}
