package net.hollowcube.mapmaker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import net.hollowcube.mapmaker.type.ServerSprite;
import net.hollowcube.mapmaker.util.FileUtil;
import net.hollowcube.mapmaker.util.JsonUtil;
import net.hollowcube.mapmaker.util.ModelUtil;
import net.hollowcube.mapmaker.util.Templates;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SpriteTransform {
    private static final Json5 json5 = new Json5();

    private static final boolean debug = false;

    private int nextChar = '\uE000';

    public final Map<String, String> entries = new HashMap<>();

    public int getNextChar() {
        return nextChar;
    }

    public void process(@NotNull PackContext ctx) throws IOException {
        final var numberModels = setupNumberModels(ctx);
        final var overlayEntries = createOverlayEntries(ctx);

        Path guiBaseDir = ctx.resources().resolve("gui");
        try (Stream<Path> guiFile = Files.walk(guiBaseDir)) {
            List<Path> files = guiFile.sorted(Comparator.comparing(Path::toString)).toList();
            for (Path imageFile : files) {
                if (!imageFile.getFileName().toString().endsWith(".png")) continue;
                Path configFile = imageFile.resolveSibling(imageFile.getFileName().toString().replace(".png", ".json5"));

                var relative = guiBaseDir.relativize(imageFile);
                boolean canSkipConfig = (relative.toString().startsWith("store" + File.separatorChar)
                        || relative.toString().startsWith("map_browser" + File.separatorChar))
                        && !relative.toString().contains("checkout");
                if (!canSkipConfig && !Files.exists(configFile)) continue;

                String name = guiBaseDir.relativize(imageFile).toString()
                        .replace(".png", "")
                        .replace("\\", "/");
                try {
                    if (canSkipConfig) {

                        Json5Object config = new Json5Object();
                        int openIndex = name.indexOf("[");
                        if (openIndex != -1) {
                            if (name.charAt(name.length() - 1) != ']') {
                                throw new RuntimeException("Invalid sprite name: " + name);
                            }

                            String params = name.substring(openIndex + 1, name.length() - 1);
                            for (var pair : params.split(",")) {
                                int eq = pair.indexOf("=");
                                if (eq == -1) {
                                    config.addProperty(pair, true);
                                } else {
                                    config.addProperty(pair.substring(0, eq), pair.substring(eq + 1));
                                }
                            }
                            name = name.substring(0, openIndex);
                        }

                        JsonObject resultFontChar = new JsonObject();
                        ServerSprite sprite = processImage(ctx, name, Files.readAllBytes(imageFile), config, resultFontChar);
                        ctx.addFontCharacter(resultFontChar);
                        ctx.addServerSprite(sprite);
                        continue;
                    }

                    Json5Object config = json5.parse(Files.readString(configFile)).getAsJson5Object();

                    if (config.get("type").getAsString().equals("sprite")) {
                        JsonObject resultFontChar = new JsonObject();
                        var serverSprite = processImage(ctx, name, Files.readAllBytes(imageFile), config, resultFontChar);
                        ctx.addFontCharacter(resultFontChar);
                        ctx.addServerSprite(serverSprite);
                    } else if (config.get("type").getAsString().equals("item")) {
                        BufferedImage image = ImageIO.read(imageFile.toFile());
                        if (image.getWidth() != 16 || image.getHeight() != 16)
                            throw new RuntimeException("Item sprites must be 16x");

                        Consumer<JsonObject> modelEditor = null;
                        if (config.has("display")) {
                            modelEditor = obj -> obj.add("display", JsonUtil.toGson(config.getAsJson5Object("display")));
                        }

                        var itemTexture = ctx.writeTexture("item", name, Files.readAllBytes(imageFile));
                        var itemModel = ctx.writeModel(name, ModelUtil.createItemGenerated(itemTexture, modelEditor));

                        if (config.get("overlays") instanceof Json5Array array) {
                            var cases = StreamSupport.stream(array.spliterator(), false)
                                    .map(Json5Element::getAsString)
                                    .map(overlayEntries::get)
                                    .filter(Objects::nonNull)
                                    .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

                            ctx.addItemModel(
                                    name,
                                    Templates.applyObject("overlay_model", Map.of(
                                            "base", itemModel,
                                            "overlays", cases
                                    ))
                            );
                        } else {
                            ctx.addItemModel(name, ModelUtil.createBasicItem(itemModel));
                        }
                    } else if (config.get("type").getAsString().equals("numbered")) {
                        BufferedImage baseImage = ImageIO.read(imageFile.toFile());
                        if (baseImage.getWidth() != 16 || baseImage.getHeight() != 16)
                            throw new RuntimeException("Numbered sprites must be 16x");

                        String texId = ctx.writeTexture("item", name, Files.readAllBytes(imageFile));
                        String baseItemModel = ctx.writeModel(name, ModelUtil.createItemGenerated(texId));

                        var entries = new JsonArray();
                        for (int i = 0; i < config.get("max_stack").getAsInt(); i++) {
                            var entry = ModelUtil.createBasicItem(numberModels[i]);
                            entry.addProperty("threshold", i + 1);
                            entries.add(entry);
                        }

                        ctx.addItemModel(
                                name,
                                Templates.applyObject("number_model", Map.of(
                                        "base", baseItemModel,
                                        "entries", entries
                                ))
                        );
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to process " + name, e);
                }
            }
        }
    }

    private @NotNull ServerSprite processImage(@NotNull PackContext ctx, @NotNull String name, byte[] data, @NotNull Json5Object conf, @NotNull JsonObject fontConf) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));

        int width = image.getWidth();
        int height = image.getHeight();
        int ascent = 0;
        int offX = 0;

        if (conf.has("size")) {
            Json5Array origin = conf.getAsJson5Array("size");
            width = origin.get(0).getAsInt();
            height = origin.get(1).getAsInt();
        }

        if (conf.has("shift_y")) {
            int shiftY = conf.get("shift_y").getAsInt();
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight() + shiftY, BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = newImage.getGraphics();
            graphics.drawImage(image, 0, shiftY, null);
            image = newImage;
            height += shiftY;
        }

        if (conf.has("origin")) {
            Json5Array origin = conf.getAsJson5Array("origin");
            offX += origin.get(0).getAsInt();
            ascent += origin.get(1).getAsInt();
        }

        // Check for empty pixels on the right side
        // Minecraft will slice off any empty rows on the right side of font characters (so that bitmaps work
        // correctly as fonts with variable width), but this is bad for us because we want the textures to
        // stay as configured. We have a special "rightOffset" property to fix this.
        int right = 0;
        outer:
        for (int x = image.getWidth() - 1; x > 0; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                int zz = image.getRGB(x, y);
                int alpha = (zz >> 24) & 0xFF;
                if (alpha != 0) {
                    break outer;
                }
            }
            right++;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String ref = ctx.writeTexture(null, name, baos.toByteArray());

        int rawFontChar;
        if (conf.has("char")) {
            rawFontChar = conf.get("char").getAsString().charAt(0);
        } else {
            rawFontChar = nextChar++;
        }
        String fontChar = String.valueOf((char) rawFontChar);//String.format("\\u%04x", nextChar++);

        fontConf.addProperty("type", "bitmap");
        fontConf.addProperty("file", ref);
        fontConf.addProperty("ascent", ascent);
        fontConf.addProperty("height", height);
        JsonArray chars = new JsonArray();
        chars.add(fontChar);
        fontConf.add("chars", chars);

        entries.put(name, fontChar);

        return new ServerSprite(name, rawFontChar, width, offX, right);
    }

    private static String[] setupNumberModels(@NotNull PackContext ctx) throws IOException {
        String[] numberModels = new String[32];
        for (int i = 0; i < numberModels.length; i++) {
            try (var img = SpriteTransform.class.getResourceAsStream("/numbers/" + (i + 1) + ".png")) {
                Objects.requireNonNull(img);

                var numberImage = ImageIO.read(img);
                var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                var g = image.createGraphics();
                int x = 16 - numberImage.getWidth(), y = 16 - numberImage.getHeight();
                g.drawImage(numberImage, x, y, numberImage.getWidth(), numberImage.getHeight(), null);
                g.dispose();
                var baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);

                var textureId = ctx.writeTexture("item", "number_" + (i + 1), baos.toByteArray());
                numberModels[i] = ctx.writeModel("number_" + (i + 1), ModelUtil.createItemGenerated(textureId, m -> {
                    var display = new JsonObject();
                    var gui = new JsonObject();
                    var translation = new JsonArray();
                    translation.add(1);
                    translation.add(-1);
                    translation.add(0);
                    gui.add("translation", translation);
                    display.add("gui", gui);
                    m.add("display", display);
                }));
            }
        }
        return numberModels;
    }

    private static Map<String, JsonElement> createOverlayEntries(@NotNull PackContext ctx) throws IOException {
        var cases = new HashMap<String, JsonElement>();
        FileUtil.walkResourcesDirectory("/overlays/", (file, stream) -> {
            var name = file.substring(file.lastIndexOf('/') + 1, file.lastIndexOf('.'));
            var id = ctx.writeTexture("item", "overlay_" + name, stream.readAllBytes());
            var entry = ModelUtil.createBasicItem(ctx.writeModel(
                    "overlay_" + name,
                    ModelUtil.createItemGenerated(id, model -> {
                        var display = new JsonObject();
                        var gui = new JsonObject();
                        var translation = new JsonArray();
                        translation.add(0);
                        translation.add(0);
                        translation.add(100);
                        gui.add("translation", translation);
                        display.add("gui", gui);
                        model.add("display", display);
                    })
            ));
            entry.addProperty("when", name);
            cases.put(name, entry);
        });

        var mcPath = ctx.vanilla().resolve("assets/minecraft/items/");
        try (var model = Files.walk(mcPath)) {
            var vanillaModels = new JsonArray();
            for (var path : model.toList()) {
                var filename = path.getFileName().toString();
                if (!filename.endsWith(".json")) continue;
                var name = filename.replace(".json", "");
                var json = FileUtil.getJson(path).getAsJsonObject();
                json.addProperty("when", name);
                vanillaModels.add(JsonUtil.stripMinecraftNamespace(json));
            }

            ctx.addItemModel(
                    "vanilla_item",
                    Templates.applyObject("vanilla_overlay_model", Map.of(
                            "cases", vanillaModels,
                            "overlays", cases.values().stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll)
                    ))
            );
        }

        return cases;
    }
}
