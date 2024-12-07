package net.hollowcube.mapmaker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class ModelTransform {
    private static final Json5 json5 = new Json5();

    private int nextChar = 0;

    public void init(@NotNull PackContext ctx, @NotNull FontTransform fontTransform) throws IOException {
        nextChar = fontTransform.getNextChar();

    }

    public void process(@NotNull PackContext ctx) throws IOException {
        // TODO(1.21.4)
//        Path fontBaseDir = ctx.resources().resolve("models");
//        try (Stream<Path> fontFileSet = Files.walk(fontBaseDir)) {
//            List<Path> files = fontFileSet.sorted(Comparator.comparing(Path::toString)).toList();
//            for (Path itemModelFile : files) {
//                if (!itemModelFile.getFileName().toString().endsWith(".json5")) continue;
//
//                String name = itemModelFile.getFileName().toString().replace(".json5", "");
//                Json5Object config = json5.parse(Files.readString(itemModelFile)).getAsJson5Object();
//                String type = config.get("type").getAsString();
//                if (!"basic".equals(type)) throw new IllegalArgumentException("Invalid model type: " + type);
//
//                // texture
//                byte[] texture = Files.readAllBytes(itemModelFile.resolveSibling(name + ".png"));
//                String texId = ctx.writeTexture("item", name, texture);
//
//                // Copy the 3d model
//                JsonObject modelObj = new Gson().fromJson(Files.readString(itemModelFile.resolveSibling(name + "_model.json")), JsonObject.class);
//                fixModelTextures(modelObj, texId);
//                String model = ctx.writeModel(name, modelObj);
//                int cmd = ctx.addBasicItem(ModelType.COLORED, name, model);
//
//                JsonObject serverSpriteConf = new JsonObject();
//                String fullName = "models/" + fontBaseDir.relativize(itemModelFile).toString().replace(".json5", "").replace("\\", "/");
////                System.out.println("processing " + fullName);
//                serverSpriteConf.addProperty("name", fullName);
//                serverSpriteConf.addProperty("cmd", cmd);
//                serverSpriteConf.addProperty("width", 0);
//                serverSpriteConf.addProperty("offsetX", 0);
//                ctx.getServerSprites().add(serverSpriteConf);
//            }
//        }
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
