package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Object;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ItemModelTransform {
    private static final Json5 json5 = new Json5();

    private int nextChar = 0;

    public void init(@NotNull PackContext ctx, @NotNull FontTransform fontTransform) throws IOException {
        nextChar = fontTransform.getNextChar();

    }

    public void process(@NotNull PackContext ctx) throws IOException {
        Path fontBaseDir = ctx.resources().resolve("item_models");
        try (Stream<Path> fontFileSet = Files.walk(fontBaseDir)) {
            List<Path> files = fontFileSet.sorted(Comparator.comparing(Path::toString)).toList();
            for (Path itemModelFile : files) {
                if (!itemModelFile.getFileName().toString().endsWith(".json5")) continue;

                String name = itemModelFile.getFileName().toString().replace(".json5", "");
                Json5Object config = json5.parse(Files.readString(itemModelFile)).getAsJson5Object();
                String type = config.get("type").getAsString();
                if (!"item_model".equals(type)) throw new IllegalArgumentException("Invalid item model type: " + type);

                // Copy the textures
                byte[] img2d = setAlpha(Files.readAllBytes(itemModelFile.resolveSibling(name + "_2d.png")), 254);
                String tex2d = ctx.writeTexture("item", name + "_2d", img2d);
                byte[] img3d = setAlpha(Files.readAllBytes(itemModelFile.resolveSibling(name + "_3d.png")), 255);
                String tex3d = ctx.writeTexture("item", name + "_3d", img3d);

                // Fix the 3d model
                JsonObject modelObj = new Gson().fromJson(Files.readString(itemModelFile.resolveSibling(name + "_3d.json")), JsonObject.class);
                add2dModel(modelObj, tex2d, tex3d);
                String model = ctx.writeModel(name, modelObj);
                int cmd = ctx.addBasicItem(name, model);

                JsonObject serverSpriteConf = new JsonObject();
                serverSpriteConf.addProperty("name", name);
                serverSpriteConf.addProperty("cmd", cmd);
                serverSpriteConf.addProperty("width", 0);
                serverSpriteConf.addProperty("offsetX", 0);
                ctx.getServerSprites().add(serverSpriteConf);
            }
        }
    }

    private void add2dModel(@NotNull JsonObject model, @NotNull String tex2d, @NotNull String tex3d) {
        JsonObject textures = model.getAsJsonObject("textures");
        String oldTextureName = findReplaceableTexture(textures);

        textures.remove(oldTextureName);
        textures.addProperty("3d", tex3d);
        textures.addProperty("2d", tex2d);
        textures.addProperty("particle", tex2d);

        JsonArray elements = model.getAsJsonArray("elements");
        for (JsonElement elem : elements) {
            JsonObject element = elem.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                face.addProperty("texture", "#3d");
            }
        }

        String elem2dStr = "{" +
                "    \"from\": [0, 0, 16]," +
                "    \"to\": [16, 16, 16]," +
                "    \"faces\": {" +
                "    \"north\": {\"uv\": [0, 0, 16, 16], \"texture\": \"#2d\"}," +
                "    \"east\": {\"uv\": [0, 0, 0, 8], \"texture\": \"#missing\"}," +
                "    \"south\": {\"uv\": [0, 0, 16, 16], \"texture\": \"#2d\"}," +
                "    \"west\": {\"uv\": [0, 0, 0, 8], \"texture\": \"#missing\"}," +
                "    \"up\": {\"uv\": [0, 0, 8, 0], \"texture\": \"#missing\"}," +
                "    \"down\": {\"uv\": [0, 0, 8, 0], \"texture\": \"#missing\"}" +
                "}}";
        JsonObject elem2d = new Gson().fromJson(elem2dStr, JsonObject.class);
        elements.add(elem2d);
    }

    private byte[] setAlpha(byte[] data, int alpha) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < src.getWidth(); x++) {
            for (int y = 0; y < src.getHeight(); y++) {
                int rgb = src.getRGB(x, y);
                if ((rgb & 0xFF000000) == 0) continue;
                dst.setRGB(x, y, (src.getRGB(x, y) & 0xFFFFFF) | (alpha << 24));
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dst, "png", baos);
        return baos.toByteArray();
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
