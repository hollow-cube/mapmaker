package net.hollowcube.mapmaker.lang;

import net.hollowcube.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class LanguageProvider {
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = LanguageProvider.class.getResourceAsStream("/lang/en_US.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (var name : properties.stringPropertyNames()) {
            var value = properties.getProperty(name);
            if (!value.contains("\n")) continue;

            properties.remove(name);
            var lines = value.split("\n");
            for (int i = 0; i < lines.length; i++) {
                properties.setProperty(name + "." + i, lines[i]);
            }
        }
    }

    private static final Pattern ARG_PATTERN = Pattern.compile("\\{[0-9]+}");

    /**
     * Translates a component (if possible, see below).
     * <p>
     * If the component is a {@link TranslatableComponent}, it will attempt to be translated. Any arguments in the
     * component will also be templated into the translation using the {@link java.text.MessageFormat} syntax of `{0}`,
     * `{1}`, etc. Translations are parsed using MiniMessage, and may contain styling as such.
     * <p>
     * Translations are always (for now) loaded from `/lang/en_US.properties` within the classpath. This system is
     * temporary, and will be replaced with either a proxy translation system or using the Adventure translation system.
     * The problem with the adventure translation system is that it does not support MiniMessage in translation strings
     * as far as I can tell.
     *
     * @param component The component to translate
     * @return The component, or a component holding just the translation key if not found
     */

    public static @NotNull Component get(@NotNull Component component) {
        if (!(component instanceof TranslatableComponent translatable)) {
            return component;
        }

        String value = properties.getProperty(translatable.key());
        if (value == null) return Component.text(translatable.key());
        Component translated = ComponentUtil.fromStringSafe(value);
        List<Component> args = translatable.args();
        if (args.size() != 0) {
            translated = translated.replaceText(TextReplacementConfig.builder()
                    .match(ARG_PATTERN)
                    .replacement((result, builder) -> {
                        var group = result.group();
                        int index = Integer.parseInt(group.substring(1, group.length() - 1));
                        return index < args.size() ?
                                args.get(index) :
                                Component.text("$$" + index);
                    }).build());
        }
        return translated;
    }

    private static final Map<String, Component> componentCache = new HashMap<>();

    public static @NotNull Component get2(@NotNull Component component) {
        // Get, but with caching for the raw minimessage components
        // This is the fastest by around an order of magnitude according to my basic test
        if (!(component instanceof TranslatableComponent translatable)) {
            return component;
        }

        Component translated = componentCache.computeIfAbsent(translatable.key(), key -> {
            String value = properties.getProperty(translatable.key());
            if (value == null) return null;
            return ComponentUtil.fromStringSafe(value);
        });
        if (translated == null) return Component.text(translatable.key());

        List<Component> args = translatable.args();
        if (args.size() != 0) {
            translated = translated.replaceText(TextReplacementConfig.builder()
                    .match(ARG_PATTERN)
                    .replacement((result, builder) -> {
                        var group = result.group();
                        int index = Integer.parseInt(group.substring(1, group.length() - 1));
                        return index < args.size() ?
                                args.get(index) :
                                Component.text("$$" + index);
                    }).build());
        }
        return translated;
    }

    public static @NotNull Component get3(@NotNull Component component) {
        // implemented using minimessage dynamic replacements directly
        if (!(component instanceof TranslatableComponent translatable)) {
            return component;
        }
        var raw = properties.getProperty(translatable.key());
        if (raw == null) return Component.text(translatable.key());

        var resolvers = new TagResolver[translatable.args().size()];
        for (int i = 0; i < resolvers.length; i++) {
            resolvers[i] = Placeholder.component(String.valueOf(i), translatable.args().get(i));
        }
        return MiniMessage.miniMessage().deserialize(raw, resolvers);
    }

    /**
     * A workaround to having variable length translations (eg lore lines, description lines).
     * Eventually will be replaced with proxy translation, which will support newlines.
     */
    public static List<Component> createMultiTranslatable(@NotNull String key, Component... args) {
        var entries = properties.stringPropertyNames().stream()
                .filter(k -> {
                    if (!k.startsWith(key)) return false;
                    var rest = k.substring(key.length());
                    return rest.length() == 0 || rest.matches("\\.[0-9]+");
                })
                .map(k -> (Component) Component.translatable(k, args))
                .toList();
        if (entries.isEmpty()) return List.of(Component.text(key));
        return entries;
    }
}