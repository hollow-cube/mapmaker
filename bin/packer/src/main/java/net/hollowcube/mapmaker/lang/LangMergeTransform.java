package net.hollowcube.mapmaker.lang;

import net.hollowcube.mapmaker.PackContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LangMergeTransform {
    private static final System.Logger logger = System.getLogger(LangMergeTransform.class.getName());

    // Replacements keeps track of all the text replacements, resolved.
    // For example, for a sprite, the value will be the unicode font character of the sprite.
    private final Map<String, String> replacements = new HashMap<>();

    public void doit(@NotNull PackContext context) throws IOException {
        var keySources = new HashMap<String, String>();
        var result = new Properties();

        try (var langFiles = Files.walk(context.resources().resolve("lang"))) {
            for (var langFile : langFiles.toList()) {
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
                        result.setProperty(key, processValue(value));
                    }
                }
            }
        }

        try (var os = Files.newOutputStream(context.out().resolve("lang.properties"))) {
            result.store(os, "Merged lang files");
        }
    }

    private @NotNull String processValue(@NotNull String translation) {
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

        return result.toString();
    }
}
