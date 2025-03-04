package net.hollowcube.mapmaker;

import com.google.gson.*;
import de.marhali.json5.*;
import net.hollowcube.mapmaker.type.ServerSprite;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SpriteTransform {
    private static final Json5 json5 = new Json5();

    private static final boolean debug = false;

    private int nextChar = '\uE000';

    public final Map<String, String> entries = new HashMap<>();

    public int getNextChar() {
        return nextChar;
    }

    public void process(@NotNull PackContext context) throws IOException {
        Path guiBaseDir = context.resources().resolve("gui");
        try (Stream<Path> guiFile = Files.walk(guiBaseDir)) {
            List<Path> files = guiFile.sorted(Comparator.comparing(Path::toString)).toList();
            for (Path imageFile : files) {
                if (!imageFile.getFileName().toString().endsWith(".png")) continue;
                Path configFile = imageFile.resolveSibling(imageFile.getFileName().toString().replace(".png", ".json5"));
                if (!Files.exists(configFile)) continue;

                String name = guiBaseDir.relativize(imageFile).toString()
                        .replace(".png", "")
                        .replace("\\", "/");
                try {
                    Json5Object config = json5.parse(Files.readString(configFile)).getAsJson5Object();

                    if (config.get("type").getAsString().equals("sprite")) {
                        JsonObject resultFontChar = new JsonObject();
                        ServerSprite sprite = processImage(context, name, Files.readAllBytes(imageFile), config, resultFontChar);
                        context.addFontCharacter(resultFontChar);
                        context.addServerSprite(sprite);
                    } else if (config.get("type").getAsString().equals("item")) {

                        BufferedImage image = ImageIO.read(imageFile.toFile());
                        if (image.getWidth() != 16 || image.getHeight() != 16)
                            throw new RuntimeException("Item sprites must be 16x");

                        Consumer<JsonObject> modelEditor = null;
                        if (config.get("no_head") != null) {
                            modelEditor = obj -> {
                                var display = new JsonObject();
                                var head = new JsonObject();
                                var scale = new JsonArray();
                                scale.add(0);
                                scale.add(0);
                                scale.add(0);
                                head.add("scale", scale);
                                display.add("head", head);
                                obj.add("display", display);
                            };
                        } else if (config.has("display")) {
                            modelEditor = obj -> obj.add("display", toGson(config.getAsJson5Object("display")));
                        }

                        ModelType modelType = config.get("colored") != null && config.get("colored").getAsBoolean() ?
                                ModelType.COLORED : ModelType.DEFAULT;

                        String modelId = context.writeBasicModel(name, Files.readAllBytes(imageFile), modelEditor);
                        int cmd = context.addBasicItem(modelType, name, modelId);
                        context.addServerSprite(ServerSprite.itemModel(name, modelId.replace("item/", ""), cmd));
                    } else if (config.get("type").getAsString().equals("numbered")) {
                        BufferedImage baseImage = ImageIO.read(imageFile.toFile());
                        if (baseImage.getWidth() != 16 || baseImage.getHeight() != 16)
                            throw new RuntimeException("Numbered sprites must be 16x");

                        BufferedImage rescaledImage = new BufferedImage(32, 32, baseImage.getType());
                        Graphics2D g = rescaledImage.createGraphics();
                        g.drawImage(baseImage, 1, 1, 16, 16, null);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(rescaledImage, "png", baos);

                        String texId = context.writeTexture("item", name, baos.toByteArray());

                        int firstCmd = -1;
                        for (int i = 0; i <= config.get("max_stack").getAsInt(); i++) {
                            String numberedName = name + "_" + i;
                            JsonObject modelObj = new JsonObject();
                            modelObj.addProperty("parent", "item/numbered_recipe_base");
                            JsonObject textures = new JsonObject();
                            textures.addProperty("0", texId);
                            textures.addProperty("1", "item/numbers_32x/" + i);
                            modelObj.add("textures", textures);
                            String modelId = context.writeModel(numberedName, modelObj);

                            int cmd = context.addBasicItem(ModelType.DEFAULT, numberedName, modelId);
                            if (firstCmd == -1) firstCmd = cmd;
                        }

                        context.addServerSprite(ServerSprite.customModelData(name, firstCmd));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to process " + name, e);
                }
            }
        }
    }

    private ServerSprite processImage(@NotNull PackContext ctx, @NotNull String name, byte[] data, @NotNull Json5Object conf, @NotNull JsonObject fontConf) throws IOException {
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
        return ServerSprite.fontChar(name, (char) rawFontChar, width, offX, right);
    }

    private static JsonElement toGson(Json5Element element) {
        return switch (element) {
            case Json5Object obj -> {
                JsonObject ret = new JsonObject();
                for (var entry : obj.entrySet()) {
                    ret.add(entry.getKey(), toGson(entry.getValue()));
                }
                yield ret;
            }
            case Json5Array arr -> {
                JsonArray ret = new JsonArray();
                for (var value : arr) {
                    ret.add(toGson(value));
                }
                yield ret;
            }
            case Json5String str -> new JsonPrimitive(str.getAsString());
            case Json5Number num -> new JsonPrimitive(num.getAsNumber());
            case Json5Boolean bool -> new JsonPrimitive(bool.getAsBoolean());
            case Json5Null ignored -> JsonNull.INSTANCE;
            default -> throw new IllegalStateException("Unexpected value: " + element);
        };
    }
}
