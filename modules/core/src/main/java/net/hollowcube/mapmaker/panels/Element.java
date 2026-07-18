package net.hollowcube.mapmaker.panels;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.CompletableFuture;

public class Element {
    protected @UnknownNullability InventoryHost host; // Set after construction, should be careful with use.

    protected int slotWidth = 0, slotHeight = 0;
    protected int x = 0, y = 0;

    private final @Nullable Sprite[] sprites = new Sprite[2]; // 0=background, 1=foreground

    public Element(int slotWidth, int slotHeight) {
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

    public Element background(@Nullable String sprite) {
        return background(sprite, 0, 0);
    }

    public Element background(@Nullable String sprite, int x, int y) {
        return background(sprite == null ? null : new Sprite(sprite, BadSprite.require(sprite), x, y));
    }

    public Element background(@Nullable Sprite sprite) {
        sprites[0] = sprite;
        if (host != null) host.queueRedraw();
        return this;
    }

    public Element at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    // Impl

    public void build(MenuBuilder builder) {
        for (var sprite : sprites) {
            if (sprite == null) continue;
            builder.draw(sprite.offsetX(), sprite.offsetY(), sprite.sprite());
        }
    }

    public @Nullable CompletableFuture<Void> handleClick(ClickType clickType, int x, int y) {
        return null;
    }

    protected void mount(InventoryHost host, boolean isInitial) {
        if (this.host != null) throw new IllegalStateException("Element already mounted");
        this.host = host;
    }

    protected void unmount() {
        this.host = null;
    }
}
