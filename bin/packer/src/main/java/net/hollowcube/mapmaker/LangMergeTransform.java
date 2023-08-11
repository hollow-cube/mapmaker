package net.hollowcube.mapmaker;

import com.google.gson.*;
import de.marhali.json5.Json5;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LangMergeTransform {
    private static final System.Logger logger = System.getLogger(LangMergeTransform.class.getName());
    private static final Json5 json5 = new Json5();

    // Replacements keeps track of all the text replacements, resolved.
    // For example, for a sprite, the value will be the unicode font character of the sprite.
    private final Map<String, String> replacements = new HashMap<>();

    public void init(@NotNull PackContext ctx, @NotNull SpriteTransform sprites) throws IOException {
        var placeholders = json5.parse(Files.readString(ctx.resources().resolve("lang").resolve("placeholders.json5"))).getAsJson5Object();
        for (var entry : placeholders.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue().getAsJson5Object();
            var type = value.get("type").getAsString();

            if (type.equals("raw")) {
                replacements.put(key, value.get("value").getAsString());
            } else if (type.equals("sprite")) {
                var ref = value.get("ref").getAsString();
                var sprite = sprites.entries.get(ref);
                if (sprite == null) {
                    throw new RuntimeException("Unknown sprite: " + ref);
                }
                replacements.put(key, sprite);
            }
        }

    }

    public void process(@NotNull PackContext context) throws IOException {
        var keySources = new HashMap<String, String>();
        var result = new JsonObject();

        try (var langFiles = Files.walk(context.resources().resolve("lang"))) {
            var files = langFiles.sorted(Comparator.comparing(Path::toString)).toList();
            for (var langFile : files) {
                if (!langFile.getFileName().toString().endsWith(".properties")) continue;

                System.out.println(langFile.toRealPath());
                try (var is = Files.newInputStream(langFile)) {
                    var properties = new Properties();
                    properties.load(is);

                    for (var entry : properties.entrySet()) {
                        var key = entry.getKey().toString();
                        var value = entry.getValue().toString();

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

        var outLangFile = context.out().resolve("server").resolve("en_US.json");
        Files.createDirectories(outLangFile.getParent());
        try (var os = Files.newBufferedWriter(outLangFile)) {
            new Gson().toJson(result, os);
        }
    }

    @SuppressWarnings("StringSplitter")
    private @NotNull JsonElement processValue(@NotNull String translation) {
        var result = new StringBuilder();

        Pattern pattern = Pattern.compile("\\$(\\w+)");
        Matcher matcher = pattern.matcher(translation);
        while (matcher.find()) {
            var key = matcher.group(1);

            var value = replacements.get(key);
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
            var lines = result.toString().split("\n");
            var array = new JsonArray();
            for (String line : lines) {
                array.add(new JsonPrimitive(line));
            }
            return array;
        }

        return new JsonPrimitive(result.toString());
    }
}
