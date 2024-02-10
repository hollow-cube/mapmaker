package net.hollowcube.proxy;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Locale;

final class Translations {

    private static final TranslationRegistry translator = TranslationRegistry.create(Key.key("hollowcube", "translations"));

    static void init() {
        register("punishments.banned", "You are banned: {0}");
        GlobalTranslator.translator().addSource(translator);
    }

    private static void register(@NotNull String key, @NotNull String messageFormat) {
        translator.register(key, Locale.ENGLISH, new MessageFormat(messageFormat, Locale.ENGLISH));
    }
}
