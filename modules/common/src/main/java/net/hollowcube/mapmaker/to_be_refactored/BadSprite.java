package net.hollowcube.mapmaker.to_be_refactored;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//todo do not duplicate this :(
public record BadSprite(char fontChar, int cmd, int width, int offsetX, int rightOffset) {
    private static final System.Logger logger = System.getLogger(BadSprite.class.getName());

    public static final Map<String, BadSprite> SPRITE_MAP;

    public static @NotNull BadSprite require(@NotNull String path) {
        return Objects.requireNonNull(SPRITE_MAP.get(path), path);
    }

    static {
        var sprites = new HashMap<String, BadSprite>();
        try (var is = BadSprite.class.getResourceAsStream("/sprites.json")) {
            if (is != null) {
                var entries = new Gson().fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), JsonArray.class);
                for (var entry : entries) {
                    var obj = entry.getAsJsonObject();
                    var key = obj.get("name").getAsString();
                    char fontChar = 0;
                    int cmd = 0;
                    if (obj.has("fontChar"))
                        fontChar = obj.get("fontChar").getAsString().charAt(0);
                    else cmd = obj.get("cmd").getAsInt();
                    var width = obj.get("width");
                    var offsetX = obj.get("offsetX");
                    var rightOffset = obj.get("rightOffset");
                    sprites.put(key, new BadSprite(fontChar, cmd,
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
