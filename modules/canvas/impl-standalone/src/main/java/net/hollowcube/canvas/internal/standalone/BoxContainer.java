package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Future;

public class BoxContainer extends ContainerElement implements SpriteHolder {

    public enum Align {
        LTR, TTB
    }

    private final Align align;

    public BoxContainer(@NotNull ElementContext context, @Nullable String id, int width, int height, @NotNull Align align) {
        super(context, id, width, height);
        this.align = Objects.requireNonNull(align, "alignment must be specified on BoxContainer");
    }

    protected BoxContainer(@NotNull ElementContext context, @NotNull BoxContainer other) {
        super(context, other);
        this.align = other.align;
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        if (shouldDelegateDraw()) return super.getContents();

        var items = new ItemStack[width() * height()];

        if (align == Align.LTR) {
            int x = 0;
            for (var child : children()) {
                patchItemArray(this,
                        items, width(), height(),
                        child.getContents(), x, 0,
                        child.width(), child.height());
                x += child.width();
            }
        } else if (align == Align.TTB) {
            int y = 0;
            for (var child : children()) {
                patchItemArray(this,
                        items, width(), height(),
                        child.getContents(), 0, y,
                        child.width(), child.height());
                y += child.height();
            }
        } else {
            throw new IllegalStateException("Unsupported alignment: " + align);
        }

        return items;
    }

    @Override
    public void buildTitle(@NotNull FontUIBuilder sb, int componentX, int componentY) {
        drawBackgroundSprite(sb, componentX, componentY);

        if (align == Align.LTR) {
            int x = 0;
            for (var child : children()) {
                child.buildTitle(sb, componentX + x, componentY);
                x += child.width();
            }
        } else if (align == Align.TTB) {
            int y = 0;
            for (var child : children()) {
                child.buildTitle(sb, componentX, componentY + y);
                y += child.height();
            }
        } else {
            throw new IllegalStateException("Unsupported alignment: " + align);
        }
    }

    @Override
    public @Nullable Future<Void> handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (shouldIgnoreInput()) return null;

        int x = slot % width(), y = slot / width();
        if (align == Align.LTR) {
            for (var child : children()) {
                if (x < child.width()) {
                    if (y >= child.height())
                        return null; // TODO: Weirdness, if you have a spacer element at the top of the view, it always fails to proceed?
                    return child.handleClick(player, y * child.width() + x, clickType);
                }

                x -= child.width();
            }
        } else if (align == Align.TTB) {
            for (var child : children()) {
                if (y < child.height()) {
                    if (x >= child.width()) return null;
                    return child.handleClick(player, y * child.width() + x, clickType);
                }

                y -= child.height();
            }
        } else {
            throw new IllegalStateException("Unsupported alignment: " + align);
        }
        return null;
    }

    @Override
    public @NotNull BoxContainer clone(@NotNull ElementContext context) {
        return new BoxContainer(context, this);
    }

}
