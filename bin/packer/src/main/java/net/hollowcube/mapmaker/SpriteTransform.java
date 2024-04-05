package net.hollowcube.mapmaker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Object;
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
import java.util.concurrent.ThreadLocalRandom;
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
//                System.out.println("processing " + name);
                try {
                    Json5Object config = json5.parse(Files.readString(configFile)).getAsJson5Object();

                    if (config.get("type").getAsString().equals("sprite")) {
                        JsonObject resultFontChar = new JsonObject();
                        JsonObject serverSpriteConf = new JsonObject();
                        processImage(context, name, Files.readAllBytes(imageFile), config, resultFontChar, serverSpriteConf);
                        context.addFontCharacter(resultFontChar);
                        context.getServerSprites().add(serverSpriteConf);
                    } else if (config.get("type").getAsString().equals("item")) {

                        BufferedImage image = ImageIO.read(imageFile.toFile());
                        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics graphics = newImage.getGraphics();
                        graphics.setColor(new Color(
                                ThreadLocalRandom.current().nextInt(0, 255),
                                ThreadLocalRandom.current().nextInt(0, 255),
                                ThreadLocalRandom.current().nextInt(0, 255),
                                255
                        ));
                        int ofwidth = image.getWidth() / 4;
                        int ofheight = image.getHeight() / 4;
                        graphics.fillRect(ofwidth, ofheight, ofwidth * 3, ofwidth * 3);
                        image = newImage;

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", baos);

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
                        }

                        int cmd;
                        if (debug) {
                            cmd = context.addBasicItemTexture(ModelType.DEFAULT, name, baos.toByteArray(), modelEditor);
                        } else {
                            cmd = context.addBasicItemTexture(ModelType.DEFAULT, name, Files.readAllBytes(imageFile), modelEditor);
                        }

                        JsonObject serverSpriteConf = new JsonObject();
                        serverSpriteConf.addProperty("name", name);
                        serverSpriteConf.addProperty("cmd", cmd);
                        serverSpriteConf.addProperty("width", 0);
                        serverSpriteConf.addProperty("offsetX", 0);
                        context.getServerSprites().add(serverSpriteConf);
                    } else if (config.get("type").getAsString().equals("numbered")) {
                        BufferedImage baseImage = ImageIO.read(imageFile.toFile());
                        if (baseImage.getWidth() != 16 || baseImage.getHeight() != 16)
                            throw new RuntimeException("Numbered sprites must be 16x");

                        BufferedImage rescaledImage = new BufferedImage(18, 18, baseImage.getType());
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
                            textures.addProperty("1", "item/numbers_18x/" + i);
                            modelObj.add("textures", textures);
                            String modelId = context.writeModel(numberedName, modelObj);

                            int cmd = context.addBasicItem(ModelType.DEFAULT, numberedName, modelId);
                            if (firstCmd == -1) firstCmd = cmd;
                        }

                        JsonObject serverSpriteConf = new JsonObject();
                        serverSpriteConf.addProperty("name", name);
                        serverSpriteConf.addProperty("cmd", firstCmd);
                        serverSpriteConf.addProperty("width", 0);
                        serverSpriteConf.addProperty("offsetX", 0);
                        context.getServerSprites().add(serverSpriteConf);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to process " + name, e);
                }
            }
        }
    }

    private void processImage(@NotNull PackContext ctx, @NotNull String name, byte[] data, @NotNull Json5Object conf, @NotNull JsonObject fontConf, @NotNull JsonObject serverSpriteConf) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));

        int width = image.getWidth();
        int height = image.getHeight();
        int ascent = 0;
        int offX = 0;

        if (debug) {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = newImage.getGraphics();
            graphics.setColor(new Color(
                    ThreadLocalRandom.current().nextInt(0, 255),
                    ThreadLocalRandom.current().nextInt(0, 255),
                    ThreadLocalRandom.current().nextInt(0, 255),
                    255
            ));
            graphics.fillRect(0, 0, newImage.getWidth(), 2);
            graphics.fillRect(newImage.getWidth() - 2, 0, newImage.getWidth(), newImage.getHeight());
            graphics.fillRect(0, newImage.getHeight() - 2, newImage.getWidth(), newImage.getHeight());
            graphics.fillRect(0, 0, 2, newImage.getHeight());
            image = newImage;
        }

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

        serverSpriteConf.addProperty("name", name);
        serverSpriteConf.addProperty("fontChar", (char) rawFontChar);
        serverSpriteConf.addProperty("width", width);
        serverSpriteConf.addProperty("offsetX", offX);
        if (right != 0)
            serverSpriteConf.addProperty("rightOffset", right);

        entries.put(name, fontChar);
    }

}
