package net.hollowcube.mapmaker.util;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class ModelUtil {
    private ModelUtil() {
    }

    public static JsonObject createItemGenerated(String texturePath) {
        return createItemGenerated(texturePath, null);
    }

    public static JsonObject createItemGenerated(String texturePath, @Nullable Consumer<JsonObject> editor) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texturePath);
        model.add("textures", textures);

        if (editor != null) editor.accept(model);

        return model;
    }

    public static JsonObject createBasicItem(String modelPath) {
        JsonObject root = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("type", "model");
        model.addProperty("model", modelPath);
        root.add("model", model);
        return root;
    }

}
