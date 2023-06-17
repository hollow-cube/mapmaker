package net.hollowcube.canvas.internal.standalone.sprite;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public record Sprite(char fontChar, int cmd, int width, int offsetX) {
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
                    int cmd = 0;
                    if (obj.has("fontChar"))
                        fontChar = obj.get("fontChar").getAsString().charAt(0);
                    else cmd = obj.get("cmd").getAsInt();
                    var width = obj.get("width").getAsInt();
                    var offsetX = obj.get("offsetX").getAsInt();
                    sprites.put(key, new Sprite(fontChar, cmd, width, offsetX));
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

//    public static Map<String, Sprite> SPRITE_MAP = Map.of(
//            "gui/play_maps/container", new Sprite('\uEff8', 182, -11),
//            "gui/play_maps/parkour_active", new Sprite('\uEff9', 54, -2),
//            "gui/build_maps/container", new Sprite('\uEffa', 256, -8),
//            "gui/build_maps/empty", new Sprite('\uEffb', 256, 71),
//            "gui/build_maps/create", new Sprite('\uEffc', 256, 71),
//            "gui/build_maps/edit", new Sprite('\uEffd', 256, 71),
//            "gui/build_maps/details", new Sprite('\uEffe', 256, -8)
//    );
}
