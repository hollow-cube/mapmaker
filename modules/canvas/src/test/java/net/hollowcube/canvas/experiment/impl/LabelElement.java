package net.hollowcube.canvas.experiment.impl;

import net.hollowcube.canvas.ItemSection;
import net.hollowcube.canvas.experiment.Label;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LabelElement extends ItemSection implements Element, Label {
    private final String id;
    private final String translationKey;

    private boolean loading = false;
    private ItemStack cachedItem = null;

    public LabelElement(@Nullable String id, int width, int height,
                        @NotNull String translationKey, @NotNull Component... args) {
        super(width, height);
        this.id = id;
        this.translationKey = translationKey;

        updateItem(args);
    }

    @Override
    public @Nullable String id() {
        return id;
    }

    @Override
    public void setArgs(@NotNull Component... args) {
        updateItem(args);
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
        if (loading) {
            setItem(ItemStack.of(Material.BARRIER));
        } else {
            setItem(cachedItem);
        }
    }

    private void updateItem(@NotNull Component... args) {
        cachedItem = ItemStack.builder(Material.PAPER)
                .displayName(Component.translatable(translationKey + ".name", args))
                .lore(LanguageProvider.optionalMultiTranslatable(translationKey + ".lore", List.of(args)))
                .build();
        if (!loading) {
            setItem(cachedItem);
        }
    }

    private void setItem(@NotNull ItemStack itemStack) {
        for (int i = 0; i < width() * height(); i++) {
            setItem(i, itemStack);
        }
    }
}
