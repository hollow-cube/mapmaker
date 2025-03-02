package net.hollowcube.mapmaker.to_be_refactored;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//todo do not duplicate this :(
public record BadSprite(char fontChar, int cmd, String model, int width, int offsetX, int rightOffset) {
    private static final System.Logger logger = System.getLogger(BadSprite.class.getName());

    public static final Map<String, BadSprite> SPRITE_MAP;
    public static final Int2ObjectMap<String> CMD_ID_MAP;
    public static final Object2IntMap<String> ID_CMD_MAP;

    public static @NotNull BadSprite require(@NotNull String path) {
        return Objects.requireNonNull(SPRITE_MAP.get(path), path);
    }

    public static @Nullable String getCmdId(@Nullable Integer cmd) {
        if (cmd == null) return null;
        return CMD_ID_MAP.get((int) cmd);
    }

    public static int getIdCmd(@NotNull String id) {
        return ID_CMD_MAP.getInt(id);
    }

    static {
        var sprites = new HashMap<String, BadSprite>();
        var cmdIdMap = new Int2ObjectArrayMap<String>();
        var idCmdMap = new Object2IntArrayMap<String>();
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
                    else if (obj.has("cmd")) {
                        cmd = obj.get("cmd").getAsInt();
                        cmdIdMap.put(cmd, key);
                        idCmdMap.put(key, cmd);
                    } else {
                        throw new UnsupportedOperationException("Sprite must have either fontChar or cmd: " + obj);
                    }
                    String model = null;
                    if (obj.has("model")) {
                        model = obj.get("model").getAsString();
                    }
                    var width = obj.get("width");
                    var offsetX = obj.get("offsetX");
                    var rightOffset = obj.get("rightOffset");
                    sprites.put(key, new BadSprite(fontChar, cmd, model,
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
            CMD_ID_MAP = Int2ObjectMaps.unmodifiable(cmdIdMap);
            ID_CMD_MAP = Object2IntMaps.unmodifiable(idCmdMap);
        }
    }

}
