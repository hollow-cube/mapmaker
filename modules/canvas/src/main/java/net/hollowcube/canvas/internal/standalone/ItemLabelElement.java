package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.internal.standalone.trait.DepthAware;
import net.hollowcube.canvas.section.ClickHandler;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class ItemLabelElement extends BaseItemElement implements Label, DepthAware {
    private final String translationKey;

    public ItemLabelElement(@Nullable String id, int width, int height,
                            @NotNull String translationKey, @NotNull Component... args) {
        super(id, width, height);
        this.translationKey = translationKey;

        updateItem(args);
    }

    @Override
    public void setArgs(@NotNull Component... args) {
        updateItem(args);
    }

    private void updateItem(@NotNull Component... args) {
        var itemStack = ItemStack.builder(Material.PAPER)
                .displayName(Component.translatable(translationKey + ".name", args))
                .lore(LanguageProvider.optionalMultiTranslatable(translationKey + ".lore", List.of(args)))
                .build();
        setItem(itemStack);
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        return ClickHandler.DENY;
    }

    @Override
    public BaseElement clone() {
        return new ItemLabelElement(id(), width(), height(), translationKey);
    }
}
