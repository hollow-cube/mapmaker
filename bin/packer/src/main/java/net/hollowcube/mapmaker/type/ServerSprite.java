package net.hollowcube.mapmaker.type;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public record ServerSprite(
        @NotNull String name,
        int cmd,
        int fontChar,
        int width,
        int offsetX,
        int rightOffset
) {
    public ServerSprite(@NotNull String name, int cmd) {
        this(name, cmd, 0, 0, 0, 0);
    }

    public ServerSprite(@NotNull String name, int fontChar, int width, int offsetX, int rightOffset) {
        this(name, 0, fontChar, width, offsetX, rightOffset);
    }

    public @NotNull JsonObject toJson() {
        var obj = new JsonObject();
        obj.addProperty("name", name);
        if (cmd != 0) obj.addProperty("cmd", cmd);
        if (fontChar != 0) obj.addProperty("fontChar", fontChar);
        if (width != 0) obj.addProperty("width", width);
        if (offsetX != 0) obj.addProperty("offsetX", offsetX);
        if (rightOffset != 0) obj.addProperty("rightOffset", rightOffset);
        return obj;
    }
}
