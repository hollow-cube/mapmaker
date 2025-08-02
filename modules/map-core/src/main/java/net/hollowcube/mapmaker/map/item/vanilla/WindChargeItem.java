package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.mapmaker.map.MapPlayer2;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.impl.projectile.WindChargeEntity;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.UseCooldown;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WindChargeItem extends VanillaItemHandler {

    public static final WindChargeItem INSTANCE = new WindChargeItem();

    public static @NotNull ItemStack get(int count, @Nullable UseCooldown cooldown) {
        var stack = WindChargeItem.INSTANCE.getItemStack(count);
        stack = cooldown != null ? stack.with(DataComponents.USE_COOLDOWN, cooldown) : stack.without(DataComponents.USE_COOLDOWN);
        return stack;
    }

    private WindChargeItem() {
        super(Material.WIND_CHARGE, RIGHT_CLICK_ANY);
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return; // Sanity

        var entity = WindChargeEntity.shootFromPlayerDirection(player, true);
        ((MapPlayer2) player).addOwnedEntity(entity);

        if (isFinite(click.itemStack())) {
            click.consume(1);
        }
    }

}
