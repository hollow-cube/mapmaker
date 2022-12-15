package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.mapmaker.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TranslatedButtonSection extends ButtonSection {
    public TranslatedButtonSection(@NotNull String baseTranslationKey, @NotNull List<Component> args,
                                   @NotNull Material mat) {
        super(1, 1, ItemStack.builder(mat)
                .displayName(Component.translatable(baseTranslationKey + ".name", args))
                .lore(LanguageProvider.optionalMultiTranslatable(baseTranslationKey + ".lore", args))
                .build(), (ClickHandler) null);
    }

    public TranslatedButtonSection(@NotNull String baseTranslationKey, @NotNull List<Component> args,
                                   @NotNull ItemStack baseItem) {
        super(1, 1, baseItem.with(builder -> builder
                .displayName(Component.translatable(baseTranslationKey + ".name", args))
                .lore(LanguageProvider.optionalMultiTranslatable(baseTranslationKey + ".lore", args))),
                (ClickHandler) null);
    }

    public TranslatedButtonSection(@NotNull String baseTranslationKey, @NotNull List<Component> args,
                                   @NotNull Material mat, @Nullable Runnable onClick) {
        super(1, 1, ItemStack.builder(mat)
                .displayName(Component.translatable(baseTranslationKey + ".name", args))
                .lore(LanguageProvider.optionalMultiTranslatable(baseTranslationKey + ".lore", args))
                .build(), onClick);
    }

    public TranslatedButtonSection(@NotNull String baseTranslationKey, @NotNull List<Component> args,
                                   @NotNull Material mat, @Nullable ClickHandler onClick) {
        super(1, 1, ItemStack.builder(mat)
                .displayName(Component.translatable(baseTranslationKey + ".name", args))
                .lore(LanguageProvider.optionalMultiTranslatable(baseTranslationKey + ".lore", args))
                .build(), onClick);
    }
}
