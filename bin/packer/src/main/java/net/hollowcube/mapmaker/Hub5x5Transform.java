package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Hub5x5Transform {

    private final JsonObject displayJson = new JsonObject();

    public void init(@NotNull PackContext ctx) throws IOException {
        var thirdPersonRightHand = new JsonObject();
        var translation = new JsonArray();
        translation.add(0);
        translation.add(8);
        translation.add(0);
        thirdPersonRightHand.add("translation", translation);
        displayJson.add("thirdperson_righthand", thirdPersonRightHand);
    }

    public void process(@NotNull PackContext ctx) throws IOException {
        Path baseDir = ctx.resources().resolve("hub/5x5");
        try (Stream<Path> fset = Files.walk(baseDir)) {
            List<Path> files = fset.sorted(Comparator.comparing(Path::toString)).toList();
            for (Path modelJson : files) {
                if (!modelJson.getFileName().toString().endsWith(".json")) continue;

                String name = modelJson.getFileName().toString().replace(".json", "");

                var modelContent = new Gson().fromJson(Files.readString(modelJson), JsonObject.class);
                modelContent.add("display", displayJson);

                String model = ctx.writeModel(name, modelContent);
                int cmd = ctx.addBasicItem(ModelType.COLORED, name, model);

                JsonObject serverSpriteConf = new JsonObject();
                serverSpriteConf.addProperty("name", "5x5/" + name);
                serverSpriteConf.addProperty("cmd", cmd);
                serverSpriteConf.addProperty("width", 0);
                serverSpriteConf.addProperty("offsetX", 0);
                ctx.getServerSprites().add(serverSpriteConf);
            }
        }
    }

}
