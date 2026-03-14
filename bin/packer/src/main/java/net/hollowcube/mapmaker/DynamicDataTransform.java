package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class DynamicDataTransform {
    private static final Json5 json5 = new Json5();
    private static final Json5 json5Conversion = new Json5(new Json5Options(false, false, false, 0));
    private static final Gson gson = new Gson();

    private final String name;

    public DynamicDataTransform(String name) {
        this.name = name;
    }

    public void process(PackContext ctx) throws IOException {
        JsonObject entries = new JsonObject();
        ctx.dynamicData.add(name, entries);

        Path dataBaseDir = ctx.resources().resolve(name);
        try (Stream<Path> fset = Files.walk(dataBaseDir)) {
            List<Path> files = fset.sorted(Comparator.comparing(Path::toString)).toList();
            for (Path dynamicDataFile : files) {
                if (!dynamicDataFile.getFileName().toString().endsWith(".json5")) continue;

                String name = dynamicDataFile.getFileName().toString().replace(".json5", "");
                Json5Object data = json5.parse(Files.readString(dynamicDataFile)).getAsJson5Object();


                entries.add(name, gson.fromJson(json5Conversion.serialize(data), JsonObject.class));
            }
        }
    }
}
