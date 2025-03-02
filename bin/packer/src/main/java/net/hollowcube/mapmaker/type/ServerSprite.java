package net.hollowcube.mapmaker.type;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public record ServerSprite(
        @NotNull String name,

        int cmd,
        String model,

        char fontChar,
        int width,
        int offsetX,
        int rightOffset
) {

    public static @NotNull ServerSprite customModelData(@NotNull String name, int customModelData) {
        return new ServerSprite(name, customModelData, null, (char) 0, 0, 0, 0);
    }

    public static @NotNull ServerSprite itemModel(@NotNull String name, @NotNull String model, int customModelData) {
        return new ServerSprite(name, customModelData, model, (char) 0, 0, 0, 0);
    }

    public static @NotNull ServerSprite fontChar(@NotNull String name, char fontChar, int width, int offsetX, int rightOffset) {
        return new ServerSprite(name, 0, null, fontChar, width, offsetX, rightOffset);
    }

    public @NotNull JsonObject toJson() {
        var obj = new JsonObject();
        obj.addProperty("name", name);
        if (model != null) obj.addProperty("model", model);
        if (cmd > 0) obj.addProperty("cmd", cmd);
        if (fontChar != 0) obj.addProperty("fontChar", fontChar);
        if (width != 0) obj.addProperty("width", width);
        if (offsetX != 0) obj.addProperty("offsetX", offsetX);
        if (rightOffset != 0) obj.addProperty("rightOffset", rightOffset);
        return obj;
    }
}
