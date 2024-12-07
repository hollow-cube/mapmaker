package net.hollowcube.mapmaker.util;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class ModelUtil {

    public static @NotNull JsonObject createItemGenerated(@NotNull String texturePath) {
        return createItemGenerated(texturePath, null);
    }

    public static @NotNull JsonObject createItemGenerated(@NotNull String texturePath, @Nullable Consumer<JsonObject> editor) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texturePath);
        model.add("textures", textures);

        if (editor != null) editor.accept(model);

        return model;
    }

    public static @NotNull JsonObject createBasicItem(@NotNull String modelPath) {
        JsonObject root = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelPath);
        root.add("model", model);
        return root;
    }

}
