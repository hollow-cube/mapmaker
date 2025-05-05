package net.hollowcube.mapmaker.panels;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Element {
    public record Sprite(@NotNull String name, @NotNull BadSprite sprite, @Nullable BadSprite hoverSprite, int x,
                         int y) {
    }

    protected InventoryHost host; // Set after construction, should be careful with use.

    protected int slotWidth = 0, slotHeight = 0;
    protected int x = 0, y = 0;

    private final Sprite[] sprites = new Sprite[2]; // 0=background, 1=foreground

    protected Element(int slotWidth, int slotHeight) {
        this.slotWidth = slotWidth;
        this.slotHeight = slotHeight;
    }

    public @Nullable Sprite sprite() {
        return sprites[1];
    }

    public @Nullable Sprite background() {
        return sprites[0];
    }

    // Builder

    public @NotNull Element background(@Nullable String sprite) {
        return background(sprite, 0, 0);
    }

    public @NotNull Element background(@Nullable String sprite, int x, int y) {
        sprites[0] = sprite == null ? null : new Sprite(sprite, BadSprite.require(sprite), null, x, y);
        if (host != null) host.queueRedraw();
        return this;
    }

    public Element at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    // Impl

    public void build(@NotNull MenuBuilder builder) {
        for (var sprite : sprites) {
            if (sprite == null) continue;
            builder.draw(sprite.x, sprite.y, sprite.sprite);

            if (sprite.hoverSprite != null) {
                var withHoverIcon = Component.text(sprite.hoverSprite.fontChar())
                        .color(FontUtil.computeShadowPos(FontUtil.Size.S3X3, builder.absoluteX(), builder.absoluteY()))
                        .shadowColor(ShadowColor.none())
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(FontUtil.computeOffset(-sprite.hoverSprite.width() - 1)));
                builder.editSlots(0, 0, builder.availWidth(), builder.availHeight(), DataComponents.CUSTOM_NAME, (Function<Component, Component>)
                        old -> withHoverIcon.append(Objects.requireNonNullElse(old, Component.empty())));
            }
        }
    }

    public @Nullable CompletableFuture<Void> handleClick(@NotNull Player player, @NotNull ClickType clickType, int x, int y) {
        return null;
    }

    protected void mount(@NotNull InventoryHost host) {
        if (this.host != null) throw new IllegalStateException("Element already mounted");
        this.host = host;
    }

    protected void unmount() {
        this.host = null;
    }
}
