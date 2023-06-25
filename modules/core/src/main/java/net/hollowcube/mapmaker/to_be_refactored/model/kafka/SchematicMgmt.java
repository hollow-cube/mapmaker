package net.hollowcube.mapmaker.to_be_refactored.model.kafka;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

public record SchematicMgmt(
        @NotNull String origin,
        long timestamp,
        @MagicConstant(valuesFromClass = SchematicMgmt.class) int action,

        @NotNull String id,
        @NotNull String name,
        @NotNull String owner,

        @Nullable String data
) {
    private static final Gson gson = new GsonBuilder().create();

    public static final int ACTION_UPLOAD = 0;
    public static final int ACTION_DOWNLOAD = 1;

    public static @NotNull SchematicMgmt fromJson(@NotNull String json) {
        return gson.fromJson(json, SchematicMgmt.class);
    }

    public SchematicMgmt(@NotNull String origin, long timestamp, @MagicConstant(valuesFromClass = SchematicMgmt.class) int action, @NotNull String id, @NotNull String name, @NotNull String owner) {
        this(origin, timestamp, action, id, name, owner, (String) null);
    }

    public SchematicMgmt(@NotNull String origin, long timestamp, @MagicConstant(valuesFromClass = SchematicMgmt.class) int action, @NotNull String id, @NotNull String name, @NotNull String owner, byte @Nullable [] data) {
        this(origin, timestamp, action, id, name, owner, data == null ? null : Base64.getEncoder().encodeToString(data));
    }

    public byte @Nullable [] dataArray() {
        return data == null ? null : Base64.getDecoder().decode(data);
    }

    public @NotNull String toJson() {
        return gson.toJson(this);
    }
}
