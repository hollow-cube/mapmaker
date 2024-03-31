package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import net.hollowcube.mapmaker.type.ServerSprite;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class CosmeticV2Transform {
    private static final Map<String, List<String>> COSMETIC_TYPES = Map.ofEntries(
            Map.entry("hat", List.of("model.json", "model.png", "icon.png")),
            Map.entry("accessory", List.of("model.json", "model.png", "icon.png")),
            Map.entry("particle", List.of("icon.png")),
            Map.entry("victory_effect", List.of("icon.png"))
    );

    private static final Gson GSON = new Gson();
    private static final Json5 json5 = new Json5();

    private BufferedImage lockOverlay;

    private int nextChar = 0;

    public void init(@NotNull PackContext ctx, @NotNull FontTransform fontTransform) throws IOException {
        nextChar = fontTransform.getNextChar();

        try (var lockOverlay = getClass().getResourceAsStream("/lock_overlay_16x.png")) {
            if (lockOverlay == null) throw new IllegalStateException("lock_overlay_16x.png not found");
            this.lockOverlay = ImageIO.read(lockOverlay);
        }
    }

    public void process(@NotNull PackContext ctx) throws IOException {
        Path baseDirectory = ctx.resources().resolve("cosmetic");
        var types = new ArrayList<>(COSMETIC_TYPES.keySet());
        types.sort(Comparator.naturalOrder());
        for (var cosmeticType : types) {
            var cosmeticTypeDir = baseDirectory.resolve(cosmeticType);
            if (!Files.exists(cosmeticTypeDir)) continue;

            try (Stream<Path> fset = Files.list(cosmeticTypeDir)) {
                loop:
                for (Path cosmeticDir : fset.sorted(Comparator.comparing(Path::toString)).toList()) {
                    if (cosmeticDir.equals(cosmeticTypeDir)) continue;

                    // Ensure the relevant files are present
                    var path = "cosmetic/" + cosmeticType + "/" + cosmeticTypeDir.relativize(cosmeticDir).toString();
                    var name = path.replace("/", "_").toLowerCase(Locale.ROOT);
                    for (var file : COSMETIC_TYPES.get(cosmeticType)) {
                        if (!Files.exists(cosmeticDir.resolve(file))) {
                            System.out.println("Missing file: " + path + "/" + file);
                            continue loop;
                        }
                    }

                    if (Files.exists(cosmeticDir.resolve("model.png"))) {   // Add resources for the 3d model
                        byte[] texture = Files.readAllBytes(cosmeticDir.resolve("model.png"));
                        String texId = ctx.writeTexture("item", name, texture);

                        JsonObject modelObj = GSON.fromJson(Files.readString(cosmeticDir.resolve("model.json")), JsonObject.class);
                        fixModelTextures(modelObj, texId);
                        String model = ctx.writeModel(name, modelObj);
                        int cmd = ctx.addBasicItem(ModelType.COLORED, name, model);

                        ctx.addServerSprite(new ServerSprite(path, cmd));
                    }

                    {   // Add icon item texture
                        byte[] texture = Files.readAllBytes(cosmeticDir.resolve("icon.png"));
                        int cmd = ctx.addBasicItemTexture(ModelType.DEFAULT, path + "/icon", texture);
                        ctx.addServerSprite(new ServerSprite(path + "/icon", cmd));

                        var tex = ImageIO.read(new ByteArrayInputStream(texture));
                        var g = tex.createGraphics();
                        g.drawImage(lockOverlay, 0, 0, null);
                        g.dispose();
                        var baos = new ByteArrayOutputStream();
                        ImageIO.write(tex, "png", baos);

                        int lockedCmd = ctx.addBasicItemTexture(ModelType.DEFAULT, path + "/icon_locked", baos.toByteArray());
                        ctx.addServerSprite(new ServerSprite(path + "/icon_locked", lockedCmd));
                    }
                }
            }
        }
    }

    private void fixModelTextures(@NotNull JsonObject model, @NotNull String texId) {
        JsonObject textures = model.getAsJsonObject("textures");
        String oldTextureName = findReplaceableTexture(textures);

        textures.remove(oldTextureName);
        textures.addProperty("1", texId);
        textures.addProperty("particle", texId);

        JsonArray elements = model.getAsJsonArray("elements");
        for (JsonElement elem : elements) {
            JsonObject element = elem.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                face.addProperty("texture", "#1");
            }
        }
    }

    private String findReplaceableTexture(@NotNull JsonObject textures) {
        if (textures.size() != 2)
            throw new IllegalArgumentException("textures must have exactly 2 entries");
        if (!textures.has("particle"))
            throw new IllegalArgumentException("textures must have a particle entry");

        for (String key : textures.keySet()) {
            if (!key.equals("particle"))
                return key;
        }

        throw new IllegalStateException("unreachable");
    }

}
