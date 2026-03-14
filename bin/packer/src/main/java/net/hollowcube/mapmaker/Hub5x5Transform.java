package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.util.ModelUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Hub5x5Transform {

    private static final List<String> NAMES = List.of("5x5", "materials", "extra");

    public void init(PackContext ctx) throws IOException {
    }

    public void process(PackContext ctx) throws IOException {
        for (var typeName : NAMES) {
            Path baseDir = ctx.resources().resolve("hub/" + typeName);

            JsonObject manifest = new Gson().fromJson(Files.readString(baseDir.resolve("_manifest.json")), JsonObject.class);

            try (Stream<Path> fset = Files.walk(baseDir)) {
                List<Path> files = fset.sorted(Comparator.comparing(Path::toString)).toList();
                for (Path modelJson : files) {
                    var fileName = modelJson.getFileName().toString();
                    if (!fileName.endsWith(".json") || fileName.startsWith("_manifest")) continue;

                    String name = modelJson.getFileName().toString().replace(".json", "");
                    var size = manifest.getAsJsonArray(name);
                    var longSize = Math.max(Math.max(size.get(0).getAsInt(), size.get(1).getAsInt()), size.get(2).getAsInt());

                    var modelContent = new Gson().fromJson(Files.readString(modelJson), JsonObject.class);
                    modelContent.add("display", createTransform(name, size));

                    var itemModelName = ctx.writeModel("hub/" + typeName + "/" + name, modelContent);
                    ctx.addItemModel("hub/" + typeName + "/" + name, ModelUtil.createBasicItem(itemModelName));
                }
            }
        }
    }

    private JsonObject createTransform(String name, JsonArray size) {
        var longSize = Math.max(Math.max(size.get(0).getAsInt(), size.get(1).getAsInt()), size.get(2).getAsInt());
        // for 5x4x5, this would be 5.

        // 16 = 1 unit
        // The model is 1 unit wide on the longest axis

        // Scale the height into 0-1
        var height = size.get(1).getAsInt() / (double) longSize;
        height /= 2; // It is centered

        var y = (height * 16); // Rescale back to model coords.

        var thirdPersonRightHand = new JsonObject();
        var translation = new JsonArray();
        translation.add(0);
        translation.add(y);
        translation.add(0);
        thirdPersonRightHand.add("translation", translation);

        var displayJson = new JsonObject();
        displayJson.add("thirdperson_righthand", thirdPersonRightHand);
        return displayJson;
    }

}
