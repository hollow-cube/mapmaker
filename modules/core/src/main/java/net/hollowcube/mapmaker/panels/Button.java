package net.hollowcube.mapmaker.panels;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.lang.MessagesBase;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.hollowcube.mapmaker.util.OverlayItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponentMap;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.item.component.CustomModelData;
import net.minestom.server.item.component.TooltipDisplay;
import net.minestom.server.network.player.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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

    protected @Nullable Component itemTitle;
    protected @Nullable List<Component> itemLore;
    protected @Nullable List<Component> itemLorePostfix;
    protected String itemModel = "minecraft:stick";
    protected @Nullable String itemOverlay = null;
    protected @Nullable ResolvableProfile itemProfile = null;
    protected @Nullable DataComponentMap extraComponents = null;
    protected @Nullable Sprite sprite;
    protected boolean disableHoverSprite = false;
    protected boolean disableTooltip = false;

    private @Nullable OnClickTypeSlot onLeftClick;
    private @Nullable OnClickTypeSlot onLeftClickAsync;
    private @Nullable OnClickTypeSlot onRightClick;
    private @Nullable OnClickTypeSlot onRightClickAsync;
    private @Nullable OnClickTypeSlot onShiftLeftClick;
    private @Nullable OnClickTypeSlot onShiftLeftClickAsync;

    public Button(int width, int height) {
        this(null, width, height);
    }

    public Button(@Nullable String translationKey, int width, int height) {
        super(width, height);
        if (translationKey != null) translationKey(translationKey);
    }

    public Button translationKey(String translationKey) {
        return translationKey(translationKey, new Object[0]);
    }

    public Button translationKey(String translationKey, Object... args) {
        //noinspection unchecked
        var translationArgs = args.length == 1 && args[0] instanceof List
            ? (List<? extends ComponentLike>) args[0]
            : MessagesBase.asArgs(args);
        this.itemTitle = LanguageProviderV2.translate(Component.translatable(translationKey + ".name", translationArgs));
        this.itemLore = LanguageProviderV2.translateMulti(translationKey + ".lore", translationArgs);

        if (host != null) host.queueRedraw();
        return this;
    }

    public Button text(Component title, List<Component> lore) {
        this.itemTitle = title;
        this.itemLore = lore;

        if (host != null) host.queueRedraw();
        return this;
    }

    public Button lorePostfix(@Nullable List<Component> lorePostfix) {
        this.itemLorePostfix = lorePostfix;

        if (host != null) host.queueRedraw();
        return this;
    }

    public Button model(String model, @Nullable String overlay) {
        if (Objects.equals(this.itemModel, model) && Objects.equals(this.itemOverlay, overlay)) return this;
        this.itemModel = model;
        this.itemOverlay = overlay;

        if (host != null) host.queueRedraw();
        return this;
    }

    public Button profile(ResolvableProfile profile) {
        if (Objects.equals(this.itemProfile, profile)) return this;
        this.itemProfile = profile;

        if (host != null) host.queueRedraw();
        return this;
    }

    public Button extraComponents(DataComponentMap extraComponents) {
        if (Objects.equals(this.extraComponents, extraComponents)) return this;
        this.extraComponents = extraComponents;

        if (host != null) host.queueRedraw();
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    public Button from(ItemStack stack) {
        var overlay = OverlayItem.getOverlay(stack);
        var base = OverlayItem.getBaseModel(stack);

        this.model(OpUtils.or(base, () -> stack.get(DataComponents.ITEM_MODEL)), overlay);
        this.text(
            Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(OpUtils.firstNonNull(
                    stack.get(DataComponents.CUSTOM_NAME),
                    stack.get(DataComponents.ITEM_NAME),
                    Component.empty()
                )),
            stack.get(DataComponents.LORE, List.of())
        );
        this.extraComponents(stack.componentPatch());

        return this;
    }

    public Button disableTooltip() {
        this.disableTooltip = true;
        return this;
    }

    // Click handling

    @Override
    public Button onLeftClick(@Nullable OnClickTypeSlot onClick) {
        this.onLeftClick = onClick;
        return this;
    }

    @Override
    public Button onLeftClickAsync(@Nullable OnClickTypeSlot onClick) {
        this.onLeftClickAsync = onClick;
        return this;
    }

    @Override
    public Button onRightClick(@Nullable OnClickTypeSlot onClick) {
        this.onRightClick = onClick;
        return this;
    }

    @Override
    public Button onRightClickAsync(@Nullable OnClickTypeSlot onClick) {
        this.onRightClickAsync = onClick;
        return this;
    }

    @Override
    public Button onShiftLeftClick(@Nullable OnClickTypeSlot onClick) {
        this.onShiftLeftClick = onClick;
        return this;
    }

    @Override
    public Button onShiftLeftClickAsync(@Nullable OnClickTypeSlot onClick) {
        this.onShiftLeftClickAsync = onClick;
        return this;
    }

    public Button sprite(@Nullable String sprite) {
        return sprite(sprite, 0, 0);
    }

    public Button sprite(@Nullable String sprite, int x, int y) {
        return sprite(sprite == null ? null : new Sprite(sprite, x, y));
    }

    public Button sprite(@Nullable Sprite sprite) {
        this.sprite = sprite;
        if (host != null) host.queueRedraw();
        return this;
    }


    // DSL overrides

    @Override
    public Button background(@Nullable String sprite) {
        return background(sprite, 0, 0);
    }

    @Override
    public Button background(@Nullable String sprite, int x, int y) {
        super.background(sprite, x, y);
        return this;
    }

    @Override
    public Button at(int x, int y) {
        super.at(x, y);
        return this;
    }

    // Impl

    @Override
    public void build(MenuBuilder builder) {
        super.build(builder);

        Component title = Objects.requireNonNullElse(this.itemTitle, Component.empty());
        if (sprite != null) {
            builder.draw(sprite.offsetX(), sprite.offsetY(), sprite.sprite());

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
        if (this.disableTooltip) {
            builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, Set.of()));
        } else {
            builder.editSlotsWithout(0, 0, slotWidth, slotHeight, DataComponents.TOOLTIP_DISPLAY);
        }
        if (!"minecraft:stick".equals(itemModel) || itemOverlay != null)
            builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.ITEM_MODEL, itemOverlay != null ? OverlayItem.OVERLAY_ITEM_MODEL : itemModel);
        if (itemProfile != null)
            builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.PROFILE, itemProfile);
        if (extraComponents != null) {
            for (var entry : extraComponents.entrySet()) {
                //noinspection unchecked
                builder.editSlots(0, 0, slotWidth, slotHeight, (DataComponent<Object>) entry.component(), entry.value());
            }
        }

        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.CUSTOM_DATA, (Function<CustomData, CustomData>) NoxesiumAPI::setImmovable);

        builder.editSlots(
            0, 0, slotWidth, slotHeight, DataComponents.CUSTOM_MODEL_DATA,
            (Function<CustomModelData, CustomModelData>) existing -> new CustomModelData(
                OpUtils.mapOr(existing, CustomModelData::floats, List.of()),
                OpUtils.mapOr(existing, CustomModelData::flags, List.of()),
                itemOverlay == null ? List.of(itemModel) : List.of(itemModel, itemOverlay),
                OpUtils.mapOr(existing, CustomModelData::colors, List.of())
            )
        );

        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.CUSTOM_NAME, title);
        var lore = itemLore;
        if (itemLorePostfix != null) {
            lore = new ArrayList<>(itemLore);
            lore.addAll(itemLorePostfix);
        }
        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.LORE, lore);
    }

    @Override
    public @Nullable CompletableFuture<Void> handleClick(ClickType clickType, int x, int y) {
        int slot = y * this.slotWidth + x;
        return switch (clickType) {
            case LEFT_CLICK -> callClickFunc(onLeftClick, onLeftClickAsync, clickType, slot);
            case SHIFT_LEFT_CLICK -> callClickFunc(onShiftLeftClick, onShiftLeftClickAsync, clickType, slot);
            case RIGHT_CLICK -> callClickFunc(onRightClick, onRightClickAsync, clickType, slot);
        };
    }

    private static @Nullable CompletableFuture<Void> callClickFunc(
        @Nullable OnClickTypeSlot func,
        @Nullable OnClickTypeSlot asyncFunc,
        ClickType clickType, int slot
    ) {
        if (func != null) {
            func.onClick(clickType, slot);
            return CompletableFuture.completedFuture(null);
        }
        if (asyncFunc != null) return FutureUtil.fork(() -> asyncFunc.onClick(clickType, slot));
        return null;
    }

}
