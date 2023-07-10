package net.hollowcube.common.lang;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class LanguageProvider {
    private static final JsonObject langData;

    static {
        JsonObject lang = new JsonObject();
        try (InputStream is = LanguageProvider.class.getResourceAsStream("/en_US.json")) {
            if (is != null) {
                lang = new Gson().fromJson(new InputStreamReader(is), JsonObject.class);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        langData = lang;
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

        var raw = langData.get(translatable.key());
        if (raw == null) return translatable;
        var value = raw.isJsonPrimitive() ? raw.getAsString() : raw.getAsJsonArray().get(0).getAsString();

        Component translated = fromStringSafe(value);
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

        //TODO MATT FIX PROPERLY OKAY? THANK YOU!!! <3
        //TODO DO NOT CACHE MISSING KEYS
        if (translatable.key().equals("chat.type.text")) {
            return component;
        }

        Component translated = componentCache.computeIfAbsent(translatable.key(), key -> {
            var raw = langData.get(translatable.key());
            if (raw == null) return null;
            var value = raw.isJsonPrimitive() ? raw.getAsString() : raw.getAsJsonArray().get(0).getAsString();

            return fromStringSafe(value);
        });
        if (translated == null) return component;

        return translateSingle(translated, translatable.args());
    }

    private static @NotNull Component translateSingle(@NotNull Component translated, @NotNull List<Component> args) {
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
        return recursiveTranslate(translated);
    }

    private static @NotNull Component recursiveTranslate(@NotNull Component component) {
        if (component instanceof TranslatableComponent translatable) {
            return get2(translatable);
        }

        var children = new ArrayList<Component>();
        for (var child : component.children())
            children.add(recursiveTranslate(child));
        return component.children(children);
    }

    public static @NotNull Component get3(@NotNull Component component) {
        // implemented using minimessage dynamic replacements directly
        if (!(component instanceof TranslatableComponent translatable)) {
            return component;
        }

        var raw = langData.get(translatable.key());
        if (raw == null) return Component.text(translatable.key());
        var value = raw.isJsonPrimitive() ? raw.getAsString() : raw.getAsJsonArray().get(0).getAsString();

        var resolvers = new TagResolver[translatable.args().size()];
        for (int i = 0; i < resolvers.length; i++) {
            resolvers[i] = Placeholder.component(String.valueOf(i), translatable.args().get(i));
        }
        return MiniMessage.miniMessage().deserialize(value, resolvers);
    }

    /**
     * A workaround to having variable length translations (eg lore lines, description lines).
     * Eventually will be replaced with proxy translation, which will support newlines.
     */
    public static List<Component> createMultiTranslatable(@NotNull String key, Component... args) {
        var entries = optionalMultiTranslatable(key, List.of(args));
        if (entries.isEmpty()) return List.of(Component.text(key));
        return entries;
    }

    /**
     * A workaround to having variable length translations (eg lore lines, description lines).
     * Eventually will be replaced with proxy translation, which will support newlines.
     */
    public static List<Component> optionalMultiTranslatable(@NotNull String key, @NotNull List<Component> args) {
        var raw = langData.get(key);
        if (raw == null) return List.of();

        JsonArray value;
        if (raw.isJsonArray()) {
            value = raw.getAsJsonArray();
        } else {
            value = new JsonArray();
            value.add(raw);
        }

        var result = new ArrayList<Component>();
        for (var entry : value) {
            result.add(translateSingle(fromStringSafe(entry.getAsString()), args));
        }
        return result;
    }

    private static @NotNull Component fromStringSafe(@NotNull String text) {
        return Component.text("", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                .append(MiniMessage.miniMessage().deserialize(text));
    }
}