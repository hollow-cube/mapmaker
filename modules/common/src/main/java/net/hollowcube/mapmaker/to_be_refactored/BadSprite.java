package net.hollowcube.mapmaker.to_be_refactored;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//todo do not duplicate this :(
public record BadSprite(char fontChar, @Nullable String model, int width, int offsetX, int rightOffset) {
    private static final System.Logger logger = System.getLogger(BadSprite.class.getName());

    public static final Map<String, BadSprite> SPRITE_MAP;
    private static final Map<String, String> MODEL_ID_MAP;
    private static final Map<String, String> ID_MODEL_MAP;

    public static BadSprite require(String path) {
        return Objects.requireNonNull(SPRITE_MAP.get(path), path);
    }

    public static @Nullable String idToModel(String id) {
        return ID_MODEL_MAP.get(id);
    }

    public static @Nullable String modelToId(@Nullable String model) {
        if (model == null) return null;
        return MODEL_ID_MAP.get(model);
    }

    public @Nullable String modelOrNull() {
        return this.model;
    }

    @Override
    public String model() {
        return Objects.requireNonNull(model);
    }

    static {
        var sprites = new HashMap<String, BadSprite>();
        var modelIdMap = new HashMap<String, String>();
        var idModelMap = new HashMap<String, String>();
        try (var is = BadSprite.class.getResourceAsStream("/sprites.json")) {
            if (is != null) {
                var entries = new Gson().fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), JsonArray.class);
                for (var entry : entries) {
                    var obj = entry.getAsJsonObject();
                    var key = obj.get("name").getAsString();
                    char fontChar = 0;
                    String model = null;
                    if (obj.has("fontChar"))
                        fontChar = (char) obj.get("fontChar").getAsInt();
                    else {
                        model = obj.get("model").getAsString();
                        modelIdMap.put(model, key);
                        idModelMap.put(key, model);
                    }
                    var width = obj.get("width");
                    var offsetX = obj.get("offsetX");
                    var rightOffset = obj.get("rightOffset");
                    sprites.put(key, new BadSprite(fontChar, model,
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
            MODEL_ID_MAP = Map.copyOf(modelIdMap);
            ID_MODEL_MAP = Map.copyOf(idModelMap);
        }
    }

}
