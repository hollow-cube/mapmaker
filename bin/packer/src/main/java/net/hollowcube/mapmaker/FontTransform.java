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
    private final Map<String, Integer> ascents = new HashMap<>();
    private final Map<String, Integer> heights = new HashMap<>();

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
            ascents.put("ascii", 7);
            heights.put("ascii", 8);
            charmaps.put("ascii_2x", charmap);
            ascents.put("ascii_2x", 7);
            heights.put("ascii_2x", 16);
            charmaps.put("ascii_4x", charmap);
            ascents.put("ascii_4x", 7);
            heights.put("ascii_4x", 32);
        }
        try (InputStream is = getClass().getResourceAsStream("/currency.json")) {
            if (is == null) throw new IOException("Failed to load currency.json");
            Json5Array fontChars = json5.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJson5Object().getAsJson5Array("chars");
            List<String> charmap = new ArrayList<>();
            for (Json5Element charset : fontChars) {
                charmap.add(charset.getAsString());
            }
            charmaps.put("currency", charmap);
            ascents.put("currency", 5);
            heights.put("currency", 6);
            charmaps.put("currency_creative", charmap);
            ascents.put("currency_creative", 5);
            heights.put("currency_creative", 6);
        }
        try (InputStream is = getClass().getResourceAsStream("/small.json")) {
            if (is == null) throw new IOException("Failed to load small.json");
            Json5Array fontChars = json5.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJson5Object().getAsJson5Array("chars");
            List<String> charmap = new ArrayList<>();
            for (Json5Element charset : fontChars) {
                charmap.add(charset.getAsString());
            }
            charmaps.put("small", charmap);
            ascents.put("small", 7);
            heights.put("small", 8);
            ascents.put("addons_tab_line1", 7);
            heights.put("addons_tab_line1", 8);
            ascents.put("addons_tab_line2", 7);
            heights.put("addons_tab_line2", 8);
        }
        try (InputStream is = getClass().getResourceAsStream("/small_tall.json")) {
            if (is == null) throw new IOException("Failed to load small_tall.json");
            Json5Array fontChars = json5.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJson5Object().getAsJson5Array("chars");
            List<String> charmap = new ArrayList<>();
            for (Json5Element charset : fontChars) {
                charmap.add(charset.getAsString());
            }
            charmaps.put("small_tall", charmap);
            ascents.put("small_tall", 24);
            heights.put("small_tall", 24);
        }
        try (InputStream is = getClass().getResourceAsStream("/gui_title.json")) {
            if (is == null) throw new IOException("Failed to load gui_title.json");
            Json5Array fontChars = json5.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJson5Object().getAsJson5Array("chars");
            List<String> charmap = new ArrayList<>();
            for (Json5Element charset : fontChars) {
                charmap.add(charset.getAsString());
            }
            charmaps.put("gui_title", charmap);
            ascents.put("gui_title", 7);
            heights.put("gui_title", 32);
            System.out.println(charmap);
        }
    }

    public int getNextChar() {
        return nextChar;
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
                fontEntry.addProperty("ascent", ascents.get(type) - config.get("y").getAsInt());
                fontEntry.addProperty("height", heights.get(type));

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
