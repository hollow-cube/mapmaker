package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Object;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SpriteTransform {
    private static final Json5 json5 = new Json5();

    private static final boolean debug = false;

    private int nextChar = '\uEff3';

    public final Map<String, String> entries = new HashMap<>();

    public int getNextChar() {
        return nextChar;
    }

    public void process(@NotNull PackContext context) throws IOException {
        var serverSprites = new JsonArray();

        var guiBaseDir = context.resources().resolve("gui");
        try (var guiFile = Files.walk(guiBaseDir)) {
            for (var imageFile : guiFile.toList()) {
                if (!imageFile.getFileName().toString().endsWith(".png")) continue;
                var configFile = imageFile.resolveSibling(imageFile.getFileName().toString().replace(".png", ".json5"));
                if (!Files.exists(configFile)) continue;

                var name = guiBaseDir.relativize(imageFile).toString().replace(".png", "");
                var config = json5.parse(Files.readString(configFile)).getAsJson5Object();
                System.out.println(name);

                if (config.get("type").getAsString().equals("sprite")) {
                    var resultFontChar = new JsonObject();
                    var serverSpriteConf = new JsonObject();
                    processImage(context, name, Files.readAllBytes(imageFile), config, resultFontChar, serverSpriteConf);
                    context.addFontCharacter(resultFontChar);
                    serverSprites.add(serverSpriteConf);
                } else if (config.get("type").getAsString().equals("item")) {

                    var image = ImageIO.read(imageFile.toFile());
                    var newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    var graphics = newImage.getGraphics();
                    graphics.setColor(new Color(
                            ThreadLocalRandom.current().nextInt(0, 255),
                            ThreadLocalRandom.current().nextInt(0, 255),
                            ThreadLocalRandom.current().nextInt(0, 255),
                            255
                    ));
                    var ofwidth = image.getWidth() / 4;
                    var ofheight = image.getHeight() / 4;
                    graphics.fillRect(ofwidth, ofheight, ofwidth * 3, ofwidth * 3);
                    image = newImage;

                    var baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "png", baos);

                    int cmd;
                    if (debug) {
                        cmd = context.addBasicItemTexture(name, baos.toByteArray());
                    } else {
                        cmd = context.addBasicItemTexture(name, Files.readAllBytes(imageFile));
                    }

                    var serverSpriteConf = new JsonObject();
                    serverSpriteConf.addProperty("name", name);
                    serverSpriteConf.addProperty("cmd", cmd);
                    serverSpriteConf.addProperty("width", 0);
                    serverSpriteConf.addProperty("offsetX", 0);
                    serverSprites.add(serverSpriteConf);
                }
            }
        }

        var serverSpritesPath = context.out().resolve("server").resolve("sprites.json");
        Files.createDirectories(serverSpritesPath.getParent());

        var gson = new Gson();
        var sprites = gson.toJson(serverSprites);
        while (sprites.contains("\\\\")) {
            // How do i do this with regex???
            sprites = sprites.replace("\\\\", "\\");
        }
        Files.writeString(serverSpritesPath, sprites);
    }

    private void processImage(@NotNull PackContext ctx, @NotNull String name, byte[] data, @NotNull Json5Object conf, @NotNull JsonObject fontConf, @NotNull JsonObject serverSpriteConf) throws IOException {
        var image = ImageIO.read(new ByteArrayInputStream(data));

        int width = image.getWidth();
        int height = image.getHeight();
        int ascent = 0;
        int offX = 0;

        if (debug) {
            var newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            var graphics = newImage.getGraphics();
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
            var origin = conf.getAsJson5Array("size");
            width = origin.get(0).getAsInt();
            height = origin.get(1).getAsInt();
        }

        if (conf.has("shift_y")) {
            var shiftY = conf.get("shift_y").getAsInt();
            var newImage = new BufferedImage(image.getWidth(), image.getHeight() + shiftY, BufferedImage.TYPE_INT_ARGB);
            var graphics = newImage.getGraphics();
            graphics.drawImage(image, 0, shiftY, null);
            image = newImage;
            height += shiftY;
        }

        if (conf.has("origin")) {
            var origin = conf.getAsJson5Array("origin");
            offX += origin.get(0).getAsInt();
            ascent += origin.get(1).getAsInt();
        }

        var baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        var ref = ctx.writeTexture(null, name, baos.toByteArray());

        var rawFontChar = nextChar++;
        var fontChar = String.valueOf((char) rawFontChar);//String.format("\\u%04x", nextChar++);

        fontConf.addProperty("type", "bitmap");
        fontConf.addProperty("file", ref);
        fontConf.addProperty("ascent", ascent);
        fontConf.addProperty("height", height);
        var chars = new JsonArray();
        chars.add(fontChar);
        fontConf.add("chars", chars);

        serverSpriteConf.addProperty("name", name);
        serverSpriteConf.addProperty("fontChar", (char) rawFontChar);
        serverSpriteConf.addProperty("width", width);
        serverSpriteConf.addProperty("offsetX", offX);

        entries.put(name, fontChar);
    }

}
