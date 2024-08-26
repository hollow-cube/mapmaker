package net.hollowcube.aj.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public record ExportedModel(
        @NotNull ModelSettings settings,
        @NotNull Map<UUID, ModelNode> nodes,
        @NotNull Map<UUID, ModelVariant> variants,
        @NotNull Map<UUID, ModelTexture> textures,
        @NotNull Map<UUID, ModelAnimation> animations
) {
    private static final Gson gson = new Gson();

    public static final Codec<ExportedModel> CODEC = RecordCodecBuilder.create(i -> i.group(
            ModelSettings.CODEC.fieldOf("settings").forGetter(ExportedModel::settings),
            Codec.unboundedMap(ExtraCodecs.UUID_STRING, ModelNode.CODEC).fieldOf("nodes").forGetter(ExportedModel::nodes),
            Codec.unboundedMap(ExtraCodecs.UUID_STRING, ModelVariant.CODEC).fieldOf("variants").forGetter(ExportedModel::variants),
            Codec.unboundedMap(ExtraCodecs.UUID_STRING, ModelTexture.CODEC).fieldOf("textures").forGetter(ExportedModel::textures),
            Codec.unboundedMap(ExtraCodecs.UUID_STRING, ModelAnimation.CODEC).fieldOf("animations").forGetter(ExportedModel::animations)
    ).apply(i, ExportedModel::new));

    public static @NotNull ExportedModel fromFile(@NotNull DynamicOps<JsonElement> ops, @NotNull Path path) {
        try {
            return fromJson(ops, gson.fromJson(Files.readString(path), JsonElement.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull ExportedModel fromJson(@NotNull DynamicOps<JsonElement> ops, @NotNull JsonElement json) {
        var result = CODEC.decode(ops, json);
        result.error().ifPresent(e -> {
            throw new IllegalArgumentException("Failed to decode model: " + e.message());
        });
        return result.result().get().getFirst();
    }
}
