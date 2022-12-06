package net.hollowcube.mapmaker.lang;

import net.hollowcube.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Naive component translation system.
 * <p>
 * Should be replaced with adventure translation or something else in the future.
 */
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
            //todo this seems like it could be wildly slow...
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

    public static @NotNull ItemStack translate(@NotNull ItemStack itemStack) {

    }
}
