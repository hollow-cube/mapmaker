package net.hollowcube.mapmaker.panels;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.lang.MessagesBase;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Button extends Element implements ButtonClickAliases {

    @FunctionalInterface
    public interface OnClick {
        void onClick();
    }

    @FunctionalInterface
    public interface OnClickType {
        void onClick(ClickType clickType);
    }

    @FunctionalInterface
    public interface OnClickTypeSlot {
        void onClick(ClickType clickType, int slot);
    }

    protected Component itemTitle;
    protected List<Component> itemLore;
    protected String itemModel = "minecraft:stick";
    protected String itemOverlay = null;
    protected Sprite sprite;
    protected boolean disableHoverSprite = false;

    private OnClickTypeSlot onLeftClick;
    private OnClickTypeSlot onLeftClickAsync;
    private OnClickTypeSlot onRightClick;
    private OnClickTypeSlot onRightClickAsync;
    private OnClickTypeSlot onShiftLeftClick;
    private OnClickTypeSlot onShiftLeftClickAsync;

    public Button(@Nullable String translationKey, int width, int height) {
        super(width, height);
        if (translationKey != null) translationKey(translationKey);
    }

    public @NotNull Button translationKey(@NotNull String translationKey) {
        return translationKey(translationKey, new Object[0]);
    }

    public @NotNull Button translationKey(@NotNull String translationKey, @NotNull Object... args) {
        var translationArgs = MessagesBase.asArgs(args);
        this.itemTitle = LanguageProviderV2.translate(Component.translatable(translationKey + ".name", translationArgs));
        this.itemLore = LanguageProviderV2.translateMulti(translationKey + ".lore", translationArgs);

        if (host != null) host.queueRedraw();
        return this;
    }

    public @NotNull Button text(@NotNull Component title, @NotNull List<Component> lore) {
        this.itemTitle = title;
        this.itemLore = lore;

        if (host != null) host.queueRedraw();
        return this;
    }

    public @NotNull Button model(@NotNull String model, @Nullable String overlay) {
        if (Objects.equals(this.itemModel, model) && Objects.equals(this.itemOverlay, overlay)) return this;
        this.itemModel = model;

        if (host != null) host.queueRedraw();
        return this;
    }

    // Click handling

    @Override
    public @NotNull Button onLeftClick(OnClickTypeSlot onClick) {
        this.onLeftClick = onClick;
        return this;
    }

    @Override
    public @NotNull Button onLeftClickAsync(OnClickTypeSlot onClick) {
        this.onLeftClickAsync = onClick;
        return this;
    }

    @Override
    public @NotNull Button onRightClick(OnClickTypeSlot onClick) {
        this.onRightClick = onClick;
        return this;
    }

    @Override
    public @NotNull Button onRightClickAsync(OnClickTypeSlot onClick) {
        this.onRightClickAsync = onClick;
        return this;
    }

    @Override
    public @NotNull Button onShiftLeftClick(OnClickTypeSlot onClick) {
        this.onShiftLeftClick = onClick;
        return this;
    }

    @Override
    public @NotNull Button onShiftLeftClickAsync(OnClickTypeSlot onClick) {
        this.onShiftLeftClickAsync = onClick;
        return this;
    }

    public @NotNull Button sprite(@Nullable String sprite) {
        return sprite(sprite, 0, 0);
    }

    public @NotNull Button sprite(@Nullable String sprite, int x, int y) {
        this.sprite = sprite == null ? null : new Sprite(sprite, BadSprite.require(sprite), BadSprite.SPRITE_MAP.get(sprite + "_hover"), x, y);
        if (host != null) host.queueRedraw();
        return this;
    }


    // DSL overrides

    @Override
    public @NotNull Button background(@Nullable String sprite) {
        return background(sprite, 0, 0);
    }

    @Override
    public @NotNull Button background(@Nullable String sprite, int x, int y) {
        super.background(sprite, x, y);
        return this;
    }

    @Override
    public @NotNull Button at(int x, int y) {
        super.at(x, y);
        return this;
    }

    // Impl

    @Override
    public void build(@NotNull MenuBuilder builder) {
        super.build(builder);

        Component title = Objects.requireNonNullElse(this.itemTitle, Component.empty());
        if (sprite != null) {
            builder.draw(sprite.x(), sprite.y(), sprite.sprite());

            if (!disableHoverSprite && sprite.hoverSprite() != null) {
                var withHoverIcon = Component.text(sprite.hoverSprite().fontChar())
                        .color(FontUtil.computeShadowPos(FontUtil.Size.fromSize(slotWidth, slotHeight), builder.absoluteX(), builder.absoluteY()))
                        .shadowColor(ShadowColor.none())
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(FontUtil.computeOffset(-sprite.hoverSprite().width() - 1)));
                title = withHoverIcon.append(title);
            }
        }

        if (this.itemModel == null || this.itemLore == null) return;
        builder.editSlotsWithout(0, 0, slotWidth, slotHeight, DataComponents.TOOLTIP_DISPLAY);
        if (!"minecraft:stick".equals(itemModel))
            builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.ITEM_MODEL, itemModel);
        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(), List.of(), itemOverlay == null ? List.of(itemModel) : List.of(itemModel, itemOverlay), List.of()
        ));
        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.CUSTOM_NAME, title);
        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.LORE, itemLore);
    }

    @Override
    public @Nullable CompletableFuture<Void> handleClick(@NotNull ClickType clickType, int x, int y) {
        int slot = y * this.slotWidth + x;
        return switch (clickType) {
            case LEFT_CLICK -> callClickFunc(onLeftClick, onLeftClickAsync, clickType, slot);
            case SHIFT_LEFT_CLICK -> callClickFunc(onShiftLeftClick, onShiftLeftClickAsync, clickType, slot);
            case RIGHT_CLICK -> callClickFunc(onRightClick, onRightClickAsync, clickType, slot);
            default -> null;
        };
    }

    private static @Nullable CompletableFuture<Void> callClickFunc(OnClickTypeSlot func, OnClickTypeSlot asyncFunc, ClickType clickType, int slot) {
        if (func != null) {
            func.onClick(clickType, slot);
            return CompletableFuture.completedFuture(null);
        }
        if (asyncFunc != null) return FutureUtil.fork(() -> asyncFunc.onClick(clickType, slot));
        return null;
    }

}
