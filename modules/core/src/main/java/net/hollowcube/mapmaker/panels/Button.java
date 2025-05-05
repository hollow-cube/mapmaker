package net.hollowcube.mapmaker.panels;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Button extends Element {
    protected String translationKey;
    protected Sprite sprite;
    protected boolean disableHoverSprite = false;

    private OnClickPlayerClickTypeSlot onLeftClick;
    private OnClickPlayerClickTypeSlot onLeftClickAsync;

    public Button(@NotNull String translationKey, int width, int height) {
        super(width, height);
        this.translationKey = translationKey;
    }

    public @NotNull Button translationKey(@NotNull String translationKey) {
        this.translationKey = translationKey;
        if (host != null) host.queueRedraw();
        return this;
    }

    // Click handling

    @FunctionalInterface
    public interface OnClickPlayer {
        void onClick(Player player);
    }

    @FunctionalInterface
    public interface OnClickPlayerClickType {
        void onClick(Player player, ClickType clickType);
    }

    @FunctionalInterface
    public interface OnClickPlayerClickTypeSlot {
        void onClick(Player player, ClickType clickType, int slot);
    }

    public @NotNull Button onLeftClick(OnClickPlayer onClick) {
        return onLeftClick((player, _, _) -> onClick.onClick(player));
    }

    public @NotNull Button onLeftClickAsync(OnClickPlayer onClick) {
        return onLeftClickAsync((player, _, _) -> onClick.onClick(player));
    }

    public @NotNull Button onLeftClick(OnClickPlayerClickType onClick) {
        return onLeftClick((player, clickType, _) -> onClick.onClick(player, clickType));
    }

    public @NotNull Button onLeftClickAsync(OnClickPlayerClickType onClick) {
        return onLeftClickAsync((player, clickType, _) -> onClick.onClick(player, clickType));
    }

    public @NotNull Button onLeftClick(OnClickPlayerClickTypeSlot onClick) {
        this.onLeftClick = onClick;
        return this;
    }

    public @NotNull Button onLeftClickAsync(OnClickPlayerClickTypeSlot onClick) {
        this.onLeftClickAsync = onClick;
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

        Component title = Component.translatable(this.translationKey + ".name");
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

        if (this.translationKey.isEmpty()) return;
        builder.editSlotsWithout(0, 0, slotWidth, slotHeight, DataComponents.TOOLTIP_DISPLAY);
        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.CUSTOM_NAME, title);
        builder.editSlots(0, 0, slotWidth, slotHeight, DataComponents.LORE, LanguageProviderV2.translateMulti(this.translationKey + ".lore", List.of()));
    }

    @Override
    public @Nullable CompletableFuture<Void> handleClick(@NotNull Player player, @NotNull ClickType clickType, int x, int y) {
        if (this.onLeftClick != null) {
            this.onLeftClick.onClick(player, clickType, y * this.slotWidth + x);
            return CompletableFuture.completedFuture(null);
        }
        if (this.onLeftClickAsync != null) {
            return FutureUtil.fork(() -> onLeftClickAsync.onClick(player, clickType, y * this.slotWidth + x));
        }
        return super.handleClick(player, clickType, x, y);
    }

}
