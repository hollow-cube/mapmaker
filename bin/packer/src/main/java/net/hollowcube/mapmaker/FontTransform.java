package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class FontTransform {
    private static final Json5 json5 = new Json5();
    private final Map<String, List<String>> charmaps = new HashMap<>();

    private int nextChar = 0;

    public void init(@NotNull PackContext ctx, @NotNull SpriteTransform spriteTransform) throws IOException {
        nextChar = spriteTransform.getNextChar();

        try (InputStream is = getClass().getResourceAsStream("/minecraft_font_default.json")) {
            if (is == null) throw new IOException("Failed to load minecraft_font_default.json");
            Json5Array fontChars = json5.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8)).getAsJson5Object()
                    .getAsJson5Array("providers")
                    .get(2).getAsJson5Object()
                    .getAsJson5Array("chars");
            List<String> charmap = new ArrayList<>();
            for (Json5Element charset : fontChars) {
                charmap.add(charset.getAsString());
            }
            charmaps.put("ascii", charmap);
            charmaps.put("ascii_2x", charmap);
        }
    }

    public void process(@NotNull PackContext ctx) throws IOException {
        JsonObject output = new JsonObject();

        Path fontBaseDir = ctx.resources().resolve("font");
        try (Stream<Path> fontFileSet = Files.walk(fontBaseDir)) {
            List<Path> files = fontFileSet.sorted(Comparator.comparing(Path::toString)).toList();
            for (Path fontFile : files) {
                if (!fontFile.getFileName().toString().endsWith(".json5")) continue;

                String name = fontFile.getFileName().toString().replace(".json5", "");
                Json5Object config = json5.parse(Files.readString(fontFile)).getAsJson5Object();
                String type = config.get("type").getAsString();

                List<String> charmap = charmaps.get(type);
                if (charmap == null) throw new RuntimeException("NO such charmap " + charmap);

                JsonObject fontEntry = new JsonObject();
                fontEntry.addProperty("__name", name);
                fontEntry.addProperty("type", "bitmap");
                fontEntry.addProperty("file", "minecraft:font/" + type + ".png");
                fontEntry.addProperty("ascent", 7 - config.get("y").getAsInt());
                if (type.equals("ascii_2x")) {
                    fontEntry.addProperty("height", 16);
                }

                JsonObject reverseCharMap = new JsonObject();

                JsonArray chars = new JsonArray();
                for (String charset : charmap) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < charset.length(); i++) {
                        char c = charset.charAt(i);
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

        Path outFile = ctx.out().resolve("server").resolve("fonts.json");
        Files.createDirectories(outFile.getParent());
        Files.writeString(outFile, new Gson().toJson(output));
    }

}
