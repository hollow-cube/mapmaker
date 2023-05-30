package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.trait.ItemSpriteHolder;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class LabelElement extends BaseElement implements Label, SpriteHolder, ItemSpriteHolder {
    private static final ItemStack BLANK_ITEM = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(System.getProperty("canvas.debug_blank").equals("1") ? 2 : 1))
            .build();

    private final String translationKey;

    private ItemStack itemSprite = BLANK_ITEM;

    public LabelElement(@NotNull ElementContext context, @Nullable String id, int width, int height, @NotNull String translationKey) {
        super(context, id, width, height);
        this.translationKey = translationKey;
        updateItem(List.of());
    }

    protected LabelElement(@NotNull ElementContext context, @NotNull LabelElement other) {
        super(context, other);
        this.translationKey = other.translationKey;
        this.itemSprite = other.itemSprite;
        updateItem(List.of());
    }

    @Override
    public void setArgs(@NotNull Component... args) {
        updateItem(List.of(args));
    }

    @Override
    public void setArgs(@NotNull List<Component> args) {
        updateItem(args);
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        if (shouldDelegateDraw()) return super.getContents();

        var contents = new ItemStack[width() * height()];
        Arrays.fill(contents, itemSprite);
        return contents;
    }

    @Override
    public void setItemSprite(@Nullable ItemStack itemStack) {
        this.itemSprite = itemStack == null ? BLANK_ITEM : itemStack;
        updateItem(List.of());
    }

    private void updateItem(@NotNull List<Component> args) {
        itemSprite = itemSprite.with(builder -> {
            builder.displayName(Component.translatable(translationKey + ".name", args));
            builder.lore(LanguageProvider.optionalMultiTranslatable(translationKey + ".lore", args));
        });
        context.markDirty();
    }

    @Override
    public @NotNull LabelElement clone(@NotNull ElementContext context) {
        return new LabelElement(context, this);
    }
}
