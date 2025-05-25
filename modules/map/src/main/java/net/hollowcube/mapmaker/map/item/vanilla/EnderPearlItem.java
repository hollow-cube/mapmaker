package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.impl.projectile.EnderPearlEntity;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnderPearlItem extends ItemHandler {
    private static final String DEFAULT_MODEL = Material.ENDER_PEARL.prototype().get(DataComponents.ITEM_MODEL);
    private static final String INFINITE_MODEL = "mapmaker:infinite_ender_pearl";
    private static final List<String> MODELS = List.of(DEFAULT_MODEL, INFINITE_MODEL);

    public static final EnderPearlItem INSTANCE = new EnderPearlItem();

    public static boolean isInfinite(@NotNull ItemStack itemStack) {
        return INFINITE_MODEL.equals(itemStack.get(DataComponents.ITEM_MODEL));
    }

    public static @NotNull ItemStack withCount(@NotNull ItemStack itemStack, int count) {
        return itemStack.with(DataComponents.MAX_STACK_SIZE, Math.max(1, count))
                .with(DataComponents.ITEM_MODEL, count == 0 ? INFINITE_MODEL : DEFAULT_MODEL)
                .withAmount(Math.max(1, count));
    }

    private EnderPearlItem() {
        super("minecraft:ender_pearl", RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable List<String> models() {
        return MODELS;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return; // Sanity

        EnderPearlEntity.shootFromPlayerDirection(player, true);

        if (!isInfinite(click.itemStack())) {
            click.updateItemStack(b -> b.amount(click.itemStack().amount() - 1));
        }
    }

}
