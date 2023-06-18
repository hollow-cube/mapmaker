package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FontTransform {
    private static final Json5 json5 = new Json5();
    private final Map<String, List<String>> charmaps = new HashMap<>();

    private int nextChar = 0;

    public void init(@NotNull PackContext ctx, @NotNull SpriteTransform spriteTransform) throws IOException {
        nextChar = spriteTransform.getNextChar();

        try (var is = getClass().getResourceAsStream("/minecraft_font_default.json")) {
            if (is == null) throw new IOException("Failed to load minecraft_font_default.json");
            var fontChars = json5.parse(is).getAsJson5Object()
                    .getAsJson5Array("providers")
                    .get(2).getAsJson5Object()
                    .getAsJson5Array("chars");
            var charmap = new ArrayList<String>();
            for (var charset : fontChars) {
                charmap.add(charset.getAsString());
            }
            charmaps.put("ascii", charmap);
            charmaps.put("ascii_2x", charmap);
        }
    }

    public void process(@NotNull PackContext ctx) throws IOException {
        var output = new JsonObject();

        var fontBaseDir = ctx.resources().resolve("font");
        try (var fontFileSet = Files.walk(fontBaseDir)) {
            var files = fontFileSet.sorted(Comparator.comparing(Path::toString)).toList();
            for (var fontFile : files) {
                if (!fontFile.getFileName().toString().endsWith(".json5")) continue;

                var name = fontFile.getFileName().toString().replace(".json5", "");
                var config = json5.parse(Files.readString(fontFile)).getAsJson5Object();
                var type = config.get("type").getAsString();

                var charmap = charmaps.get(type);
                if (charmap == null) throw new RuntimeException("NO such charmap " + charmap);

                var fontEntry = new JsonObject();
                fontEntry.addProperty("__name", name);
                fontEntry.addProperty("type", "bitmap");
                fontEntry.addProperty("file", "minecraft:font/" + type + ".png");
                fontEntry.addProperty("ascent", 7 - config.get("y").getAsInt());
                if (type.equals("ascii_2x")) {
                    fontEntry.addProperty("height", 16);
                }

                var reverseCharMap = new JsonObject();

                var chars = new JsonArray();
                for (var charset : charmap) {
                    var sb = new StringBuilder();
                    for (char c : charset.toCharArray()) {
                        if (c == 0) {
                            sb.append('\u0000');
                            continue;
                        }
                        int mappedC = nextChar++;
                        sb.append((char) mappedC);
                        reverseCharMap.addProperty(String.valueOf(c), (char) mappedC);
                    }
                    chars.add(sb.toString());
                }
                fontEntry.add("chars", chars);

                output.add(name, reverseCharMap);

                ctx.addFontCharacter(fontEntry);
            }
        }

        var outFile = ctx.out().resolve("server").resolve("fonts.json");
        Files.createDirectories(outFile.getParent());
        Files.writeString(outFile, new Gson().toJson(output));
    }

}
