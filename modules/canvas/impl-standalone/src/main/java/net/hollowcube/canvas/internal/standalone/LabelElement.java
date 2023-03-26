package net.hollowcube.canvas.internal.standalone;

import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class LabelElement extends BaseElement {

    private final String translationKey;

    public LabelElement(@Nullable String id, int width, int height, @NotNull String translationKey) {
        super(id, width, height);
        this.translationKey = translationKey;
    }

    protected LabelElement(@NotNull LabelElement other) {
        super(other);
        this.translationKey = other.translationKey;
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        var contents = new ItemStack[width() * height()];
        Arrays.fill(contents, ItemStack.of(Material.PAPER));
        return contents;
    }

    @Override
    public @NotNull LabelElement dup() {
        return new LabelElement(this);
    }
}
