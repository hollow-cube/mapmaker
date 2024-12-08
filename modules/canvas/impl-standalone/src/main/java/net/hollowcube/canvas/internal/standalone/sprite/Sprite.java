package net.hollowcube.canvas.internal.standalone.sprite;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public record Sprite(char fontChar, @Nullable String model, int width, int offsetX, int rightOffset) {
    private static final System.Logger logger = System.getLogger(Sprite.class.getName());

    public static final Map<String, Sprite> SPRITE_MAP;

    static {
        var sprites = new HashMap<String, Sprite>();
        try (var is = Sprite.class.getResourceAsStream("/sprites.json")) {
            if (is != null) {
                var entries = new Gson().fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), JsonArray.class);
                for (var entry : entries) {
                    var obj = entry.getAsJsonObject();
                    var key = obj.get("name").getAsString();
                    char fontChar = 0;
                    String model = null;
                    if (obj.has("fontChar"))
                        fontChar = (char) obj.get("fontChar").getAsInt();
                    else model = obj.get("model").getAsString();
                    var width = obj.get("width");
                    var offsetX = obj.get("offsetX");
                    var rightOffset = obj.get("rightOffset");
                    sprites.put(key, new Sprite(fontChar, model,
                            width == null ? 0 : width.getAsInt(),
                            offsetX == null ? 0 : offsetX.getAsInt(),
                            rightOffset == null ? 0 : rightOffset.getAsInt()));
                }
            } else {
                logger.log(System.Logger.Level.WARNING, "No sprites present in build");
            }
        } catch (IOException e) {
            logger.log(System.Logger.Level.ERROR, "Failed to load sprites.json", e);
        } finally {
            SPRITE_MAP = Map.copyOf(sprites);
        }
    }
}
