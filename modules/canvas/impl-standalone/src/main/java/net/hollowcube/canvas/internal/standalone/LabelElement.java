package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.trait.ItemSpriteHolder;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class LabelElement extends BaseElement implements Label, ItemSpriteHolder {
    private static final ItemStack BLANK_ITEM = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(1000))
            .build();

    private final String translationKey;

    private ItemStack itemSprite = BLANK_ITEM;

    public LabelElement(@NotNull ElementContext context, @Nullable String id, int width, int height, @NotNull String translationKey) {
        super(context, id, width, height);
        this.translationKey = translationKey;
    }

    protected LabelElement(@NotNull ElementContext context, @NotNull LabelElement other) {
        super(context, other);
        this.translationKey = other.translationKey;
        this.itemSprite = other.itemSprite;
    }

    @Override
    public void setArgs(@NotNull Component... args) {
        updateItem(args);
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        var contents = new ItemStack[width() * height()];
        Arrays.fill(contents, itemSprite);
        return contents;
    }

    @Override
    public void setItemSprite(@Nullable ItemStack itemStack) {
        this.itemSprite = itemStack == null ? BLANK_ITEM : itemStack;
        updateItem();
    }

    private void updateItem(@NotNull Component... args) {
        itemSprite = itemSprite.with(builder -> {
            builder.displayName(Component.translatable(translationKey + ".name", args));
            builder.lore(LanguageProvider.optionalMultiTranslatable(translationKey + ".lore", List.of(args)));
        });
        context.markDirty();
    }

    @Override
    public @NotNull LabelElement clone(@NotNull ElementContext context) {
        return new LabelElement(context, this);
    }
}
