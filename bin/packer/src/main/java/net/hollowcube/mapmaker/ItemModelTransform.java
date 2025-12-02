package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Object;
import net.hollowcube.mapmaker.util.ModelUtil;
import net.hollowcube.mapmaker.util.Templates;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemModelTransform {
    private static final Json5 json5 = new Json5();

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

                // Create the 2d model
                byte[] img2d = Files.readAllBytes(itemModelFile.resolveSibling(name + "_2d.png"));
                String tex2d = ctx.writeTexture("item", name + "_2d", img2d);
                String model2d = ctx.writeModel(name + "_2d", ModelUtil.createItemGenerated(tex2d));

                // Create the 3d model
                byte[] img3d = Files.readAllBytes(itemModelFile.resolveSibling(name + "_3d.png"));
                String tex3d = ctx.writeTexture("item", name + "_3d", img3d);
                JsonObject modelObj3d = new Gson().fromJson(Files.readString(itemModelFile.resolveSibling(name + "_3d.json")), JsonObject.class);
                String model3d = ctx.writeModel(name + "_3d", transform3dModel(modelObj3d, tex3d));

                // Create the composite model
                ctx.addItemModel(name, Templates.applyObject("2d_3d_composite",
                        Map.of("2d", model2d, "3d", model3d)));
            }
        }

        fontBaseDir = ctx.resources().resolve("item_models_v2");
        try (Stream<Path> fontFileSet = Files.walk(fontBaseDir)) {
            List<Path> files = fontFileSet.sorted(Comparator.comparing(Path::toString)).toList();
            for (Path itemModelFile : files) {
                if (!Files.isDirectory(itemModelFile)) continue;
                final Path modelPath = itemModelFile.resolve("model.json");
                if (!Files.exists(modelPath)) continue;

                String name = itemModelFile.getFileName().toString();
                JsonObject model = new Gson().fromJson(Files.readString(modelPath), JsonObject.class);

                var type = model.get("type");
                if (type != null && type.getAsString().equals("vanilla/block")) {
                    ctx.addItemModel(name, ModelUtil.createBasicItem("minecraft:block/" + model.get("id").getAsString()));
                    continue;
                }

                var images = Files.walk(itemModelFile)
                        .filter(path -> path.getFileName().toString().endsWith(".png"))
                        .collect(Collectors.toMap(
                                path -> path.getFileName().toString().replace(".png", ""),
                                path -> {
                                    try {
                                        return ctx.writeTexture(
                                                "item",
                                                name + "/" + path.getFileName().toString().replace(".png", ""),
                                                Files.readAllBytes(path)
                                        );
                                    } catch (IOException e) {
                                        throw new RuntimeException("Failed to read image: " + path, e);
                                    }
                                }
                        ));

                if (type != null && type.getAsString().equals("mapmaker/variant/single")) {
                    for (var entry : images.entrySet()) {
                        var modelCopy = model.deepCopy();

                        var key = entry.getKey();
                        var image = entry.getValue();

                        var newTextures = new JsonObject();
                        newTextures.addProperty("texture", image);
                        modelCopy.add("textures", newTextures);

                        ctx.addItemModel(
                                "%s_%s".formatted(name, key),
                                ModelUtil.createBasicItem(ctx.writeModel("%s_%s".formatted(name, key), modelCopy))
                        );
                    }
                } else {
                    var newTextures = new JsonObject();
                    for (var entry : model.get("textures").getAsJsonObject().entrySet())
                        newTextures.addProperty(
                                entry.getKey(),
                                images.getOrDefault(entry.getValue().getAsString(), entry.getValue().getAsString())
                        );
                    model.add("textures", newTextures);

                    var itemModelName = ctx.writeModel(name, model);
                    ctx.addItemModel(name, ModelUtil.createBasicItem(itemModelName));
                }
            }
        }
    }

    private @NotNull JsonObject transform3dModel(@NotNull JsonObject model, @NotNull String tex3d) {
        JsonObject textures = model.getAsJsonObject("textures");
        String oldTextureName = findReplaceableTexture(textures);

        textures.addProperty(oldTextureName, tex3d);
        textures.addProperty("particle", tex3d);

        return model;
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
