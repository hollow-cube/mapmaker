package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.util.OverlayItem;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public abstract class VanillaItemHandler extends ItemHandler {

    private static final String INFINITE_OVERLAY = "infinite";

    protected final Material item;

    protected VanillaItemHandler(@NotNull Material item, int... flags) {
        super(item.name(), flags);

        this.item = item;
    }

    @Override
    public void build(ItemStack.@NotNull Builder builder, @Nullable CompoundBinaryTag tag) {
        super.build(builder, tag);

        builder.material(Material.MAP);
        builder.set(DataComponents.CUSTOM_NAME, Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(Objects.requireNonNullElse(this.item.prototype().get(DataComponents.ITEM_NAME), Component.empty()))
        );
        builder.set(DataComponents.ITEM_MODEL, this.item.prototype().get(DataComponents.ITEM_MODEL));
    }

    protected ItemStack getItemStack(int count) {
        var stack = this.getItemStack();
        stack = stack.with(DataComponents.MAX_STACK_SIZE, Math.max(1, count));
        stack = OverlayItem.with(stack, this.item, count == 0 ? INFINITE_OVERLAY : null);
        stack = stack.withAmount(Math.max(1, count));
        return stack;
    }

    protected boolean isFinite(@NotNull ItemStack stack) {
        return !Objects.equals(OverlayItem.getOverlay(stack), INFINITE_OVERLAY);
    }
}
