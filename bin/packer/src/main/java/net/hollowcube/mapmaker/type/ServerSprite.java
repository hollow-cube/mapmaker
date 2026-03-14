package net.hollowcube.mapmaker.type;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public record ServerSprite(
        String name,
        @Nullable String model,
        int fontChar,
        int width,
        int offsetX,
        int rightOffset
) {

    public ServerSprite(String name, @Nullable String model) {
        this(name, model, 0, 0, 0, 0);
    }

    public ServerSprite(String name, int fontChar, int width, int offsetX, int rightOffset) {
        this(name, null, fontChar, width, offsetX, rightOffset);
    }

    public JsonObject toJson() {
        var obj = new JsonObject();
        obj.addProperty("name", name);
        if (model != null) obj.addProperty("model", model);
        if (fontChar != 0) obj.addProperty("fontChar", fontChar);
        if (width != 0) obj.addProperty("width", width);
        if (offsetX != 0) obj.addProperty("offsetX", offsetX);
        if (rightOffset != 0) obj.addProperty("rightOffset", rightOffset);
        return obj;
    }
}
