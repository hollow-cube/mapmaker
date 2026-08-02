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
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SpriteTransform {
    private static final Json5 json5 = new Json5();

    private static final boolean debug = false;

    /// Identity written into the first data pixel of a hover sprite, matched by the text shader.
    /// Green and blue are both non zero so a grayscale (single channel) font sheet can never match.
    private static final int HOVER_ICON_ID = 0xFE4E2A;

    private static final int HOVER_OUTLINE_COLOR = 0xFFFFFFFF;
    /// A button lives inside a container, so anything bigger than one is not button chrome.
    private static final int MAX_BUTTON_WIDTH = 176, MAX_BUTTON_HEIGHT = 222;

    /// Generated outlines are just rectangle borders, so the few hundred sprites that qualify
    /// collapse into a few dozen distinct glyphs. Keyed by the encoded png.
    private final Map<String, Character> outlinesByContent = new HashMap<>();

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
                                         || relative.toString().startsWith("map_browser" + File.separatorChar)
                                         || relative.toString().startsWith("map_details" + File.separatorChar)
                                         || relative.toString().startsWith("report_map" + File.separatorChar)
                                         || relative.toString().startsWith("generic2" + File.separatorChar + "containers" + File.separatorChar)
                                         || relative.toString().startsWith("action" + File.separatorChar)
                                         || relative.toString().startsWith("event" + File.separatorChar)
                                         || relative.toString().startsWith("icon2" + File.separatorChar)
                                         || relative.toString().startsWith("rate_map" + File.separatorChar)
                                         || relative.toString().startsWith("create_maps2" + File.separatorChar))
                                        && !relative.toString().contains("checkout");
                boolean useConfig = Files.exists(configFile);
                if (!canSkipConfig && !useConfig) continue;

                String name = guiBaseDir.relativize(imageFile).toString()
                    .replace(".png", "")
                    .replace("\\", "/");
                try {
                    if (canSkipConfig && !useConfig) {

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

        // Hover sprites are repositioned onto their button by the text shader, which needs to know how
        // big they are before it can rebuild the quad. Bake it into the texture rather than spending
        // marker colour bits on it.
        if (name.endsWith("_hover")) {
            image = addDataPixels(image);
            height += 2;
        } else {
            emitHoverOutline(ctx, name, image, ascent, offX);
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

    /// Emits `<name>_outline` for sprites that are a solid rectangle, ie a button face rather than an
    /// icon, so any button using one as its background gets a hover effect without art being drawn.
    ///
    /// This is deliberately a different suffix from the hand drawn `_hover`: a generated outline is
    /// only ever right for the button's background, and tracing a foreground icon would outline the
    /// icon rather than the button.
    private void emitHoverOutline(@NotNull PackContext ctx, @NotNull String name, @NotNull BufferedImage source,
                                  int ascent, int offsetX) throws IOException {
        BufferedImage outline = solidAreaBorder(source);
        if (outline == null) return;

        var baos = new ByteArrayOutputStream();
        ImageIO.write(addDataPixels(outline), "png", baos);
        String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());

        String outlineName = name + "_outline";
        Character fontChar = outlinesByContent.get(encoded);
        if (fontChar == null) {
            String ref = ctx.writeTexture(null, outlineName, baos.toByteArray());
            fontChar = (char) nextChar++;

            var fontConf = new JsonObject();
            fontConf.addProperty("type", "bitmap");
            fontConf.addProperty("file", ref);
            fontConf.addProperty("ascent", ascent);
            fontConf.addProperty("height", outline.getHeight() + 2); // the data rows
            var chars = new JsonArray();
            chars.add(String.valueOf(fontChar));
            fontConf.add("chars", chars);
            ctx.addFontCharacter(fontConf);

            outlinesByContent.put(encoded, fontChar);
        }

        entries.put(outlineName, String.valueOf(fontChar));
        // The data pixels reach both edges, so nothing is ever trimmed off the right.
        ctx.addServerSprite(new ServerSprite(outlineName, fontChar, outline.getWidth(), offsetX, 0));
    }

    /// The one pixel border of the sprite's opaque area, or null when that area has no unbroken
    /// border of its own - an icon or artwork rather than a button face.
    ///
    /// Requiring a complete border rather than a filled rectangle lets hollow chrome (a frame with a
    /// transparent middle) through while still rejecting icons, whose bounding box has gaps in it.
    private static @Nullable BufferedImage solidAreaBorder(@NotNull BufferedImage source) {
        int width = source.getWidth(), height = source.getHeight();
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isOpaque(source, x, y)) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < 0) return null;
        if (width < 4) return null; // no room for the data pixels
        if (maxX - minX + 1 > MAX_BUTTON_WIDTH || maxY - minY + 1 > MAX_BUTTON_HEIGHT) return null;

        for (int y = minY; y <= maxY; y++)
            if (!isOpaque(source, minX, y) || !isOpaque(source, maxX, y)) return null;
        for (int x = minX; x <= maxX; x++)
            if (!isOpaque(source, x, minY) || !isOpaque(source, x, maxY)) return null;

        // Kept at the source's size so the outline lands wherever the sprite itself is drawn.
        var result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++)
                if (x == minX || x == maxX || y == minY || y == maxY)
                    result.setRGB(x, y, HOVER_OUTLINE_COLOR);
        return result;
    }

    private static boolean isOpaque(@NotNull BufferedImage image, int x, int y) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return false;
        return (image.getRGB(x, y) >>> 24) != 0;
    }

    /// Adds a data row above and below the image, holding the sprite identity and its content size.
    ///
    /// Both rows carry the identity at the outermost pixel and the size one pixel further in, at both
    /// ends, so a vertex can read them by stepping inward from whichever corner of the quad it is on.
    /// The shader crops both rows back off before sampling, so they are never visible.
    private static @NotNull BufferedImage addDataPixels(@NotNull BufferedImage image) {
        int width = image.getWidth(), contentHeight = image.getHeight();
        if (width < 4) throw new RuntimeException("Sprite must be at least 4px wide to hold data pixels");
        if (width > 256 || contentHeight > 256) throw new RuntimeException("Sprite must be at most 256x256 to hold data pixels");

        var result = new BufferedImage(width, contentHeight + 2, BufferedImage.TYPE_INT_ARGB);
        var graphics = result.getGraphics();
        graphics.drawImage(image, 0, 1, null);
        graphics.dispose();

        int identity = 0xFF000000 | HOVER_ICON_ID;
        int size = 0xFF000000 | ((width - 1) << 16) | ((contentHeight - 1) << 8);
        for (int y : new int[]{0, result.getHeight() - 1}) {
            result.setRGB(0, y, identity);
            result.setRGB(1, y, size);
            result.setRGB(width - 2, y, size);
            result.setRGB(width - 1, y, identity);
        }
        return result;
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

        var overlaysJson = cases.values().stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        var versionedModels = new HashMap<String, JsonObject>();

        for (var version : ctx.versions()) {
            var mcPath = ctx.vanilla(version).resolve("assets/minecraft/items/");
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

                versionedModels.put(version, Templates.applyObject("vanilla_overlay_model", Map.of(
                    "cases", vanillaModels,
                    "overlays", overlaysJson
                )));
            }
        }

        ctx.addItemModels("vanilla_item", versionedModels);

        return cases;
    }
}
