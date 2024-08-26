package net.hollowcube.aj.resourcepack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.hollowcube.aj.model.ExportedModel;
import net.hollowcube.aj.model.ModelVariant;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ResourcePackExporter {
    private static final Gson GSON = new Gson();

    public static void export(@NotNull Path packRoot, @NotNull ExportedModel model) throws IOException {
        var namespaceAssets = packRoot.resolve("assets/animated_java");

        var textures = namespaceAssets.resolve("textures/item/" + model.settings().exportNamespace());
        for (var texture : model.textures().values()) {
            var path = textures.resolve(texture.name());
            Files.createDirectories(path.getParent());
            Files.write(path, Base64.getDecoder().decode(texture.src().substring(texture.src().indexOf(",") + 1)));
        }

        int cmdOffset = model.settings().customModelDataOffset();
        var itemOverrides = new JsonArray();

        var models = namespaceAssets.resolve("models/item/" + model.settings().exportNamespace());
        for (var variant : model.variants().values()) {
            // TODO: need to support texture_map

            for (var modelEntry : variant.models().entrySet()) {
                // Add the vanilla model override
                var itemOverride = new JsonObject();
                var predicate = new JsonObject();
                predicate.addProperty("custom_model_data", cmdOffset + modelEntry.getValue().customModelData());
                itemOverride.add("predicate", predicate);
                itemOverride.addProperty("model", "animated_java:item/" + model.settings().exportNamespace() + "/" + modelEntry.getKey());
                itemOverride.addProperty("axiom:hide", true);
                itemOverrides.add(itemOverride);

                // Write the model file
                if (modelEntry.getValue().model() instanceof ModelVariant.VanillaModel vanillaModel) {
                    var modelPath = models.resolve(modelEntry.getKey() + ".json");
                    Files.createDirectories(modelPath.getParent());
                    Files.writeString(modelPath, GSON.toJson(vanillaModel.model()));
                } else {
                    throw new UnsupportedOperationException("Only vanilla models are supported");
                }
            }
        }

        // Write the vanilla model override
        var displayItem = model.settings().displayItem().namespace();
        var vanillaItemModel = new JsonObject();
        vanillaItemModel.addProperty("parent", "item/generated");
        var texturesObject = new JsonObject();
        texturesObject.addProperty("layer0", "item/" + displayItem.path());
        vanillaItemModel.add("textures", texturesObject);
        vanillaItemModel.add("overrides", itemOverrides);

        var vanillaModelPath = packRoot.resolve("assets/" + displayItem.namespace() + "/models/item/" + displayItem.path() + ".json");
        Files.createDirectories(vanillaModelPath.getParent());
        Files.writeString(vanillaModelPath, GSON.toJson(vanillaItemModel));
    }
}
