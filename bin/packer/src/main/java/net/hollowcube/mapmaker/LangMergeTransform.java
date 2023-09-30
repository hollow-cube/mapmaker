package net.hollowcube.mapmaker;

import com.google.gson.*;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LangMergeTransform {
    private static final System.Logger logger = System.getLogger(LangMergeTransform.class.getName());
    private static final Json5 json5 = new Json5();

    // Replacements keeps track of all the text replacements, resolved.
    // For example, for a sprite, the value will be the unicode font character of the sprite.
    private final Map<String, String> replacements = new HashMap<>();

    public void init(@NotNull PackContext ctx, @NotNull SpriteTransform sprites) throws IOException {
        Json5Object placeholders = json5.parse(Files.readString(ctx.resources().resolve("lang").resolve("placeholders.json5"))).getAsJson5Object();
        for (Map.Entry<String, Json5Element> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            Json5Object value = entry.getValue().getAsJson5Object();
            String type = value.get("type").getAsString();

            if (type.equals("raw")) {
                replacements.put(key, value.get("value").getAsString());
            } else if (type.equals("sprite")) {
                String ref = value.get("ref").getAsString();
                String sprite = sprites.entries.get(ref);
                if (sprite == null) {
                    throw new RuntimeException("Unknown sprite: " + ref);
                }
                replacements.put(key, sprite);
            }
        }

    }

    public void process(@NotNull PackContext context) throws IOException {
        Map<String, String> keySources = new HashMap<>();
        JsonObject result = new JsonObject();

        try (Stream<Path> langFiles = Files.walk(context.resources().resolve("lang"))) {
            List<Path> files = langFiles.sorted(Comparator.comparing(Path::toString)).toList();
            for (Path langFile : files) {
                if (!langFile.getFileName().toString().endsWith(".properties")) continue;

                System.out.println(langFile.toRealPath());
                try (InputStream is = Files.newInputStream(langFile)) {
                    Properties properties = new Properties();
                    properties.load(is);

                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        String key = entry.getKey().toString();
                        String value = entry.getValue().toString();

                        if (keySources.containsKey(key)) {
                            logger.log(System.Logger.Level.ERROR, String.format(
                                    "Duplicate key: %s in %s (originally in %s)",
                                    key, langFile.getFileName(), keySources.get(key)
                            ));
                            continue;
                        }
                        keySources.put(key, langFile.getFileName().toString());
                        result.add(key, processValue(value));
                    }
                }
            }
        }

        Path outLangFile = context.out().resolve("server").resolve("en_US.json");
        Files.createDirectories(outLangFile.getParent());
        try (BufferedWriter os = Files.newBufferedWriter(outLangFile)) {
            new Gson().toJson(result, os);
        }
    }

    @SuppressWarnings("StringSplitter")
    private @NotNull JsonElement processValue(@NotNull String translation) {
        StringBuilder result = new StringBuilder();

        Pattern pattern = Pattern.compile("\\$(\\w+)");
        Matcher matcher = pattern.matcher(translation);
        while (matcher.find()) {
            String key = matcher.group(1);

            String value = replacements.get(key);
            if (value == null) {
                logger.log(System.Logger.Level.ERROR, String.format(
                        "Unknown key: %s", key
                ));
                value = "MISSING";
            }
            matcher.appendReplacement(result, value);
        }
        matcher.appendTail(result);

        // Convert newlines to arrays
        if (result.indexOf("\n") != -1) {
            String[] lines = result.toString().split("\n");
            JsonArray array = new JsonArray();
            for (String line : lines) {
                array.add(new JsonPrimitive(line));
            }
            return array;
        }

        return new JsonPrimitive(result.toString());
    }
}
