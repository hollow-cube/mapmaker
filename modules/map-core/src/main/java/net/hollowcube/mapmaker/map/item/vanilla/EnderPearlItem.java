package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.mapmaker.map.MapPlayer2;
import net.hollowcube.mapmaker.map.entity.impl.projectile.EnderPearlEntity;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.UseCooldown;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnderPearlItem extends VanillaItemHandler {

    public static final EnderPearlItem INSTANCE = new EnderPearlItem();

    public static @NotNull ItemStack get(int count, @Nullable UseCooldown cooldown) {
        var stack = EnderPearlItem.INSTANCE.getItemStack(count);
        stack = cooldown != null ? stack.with(DataComponents.USE_COOLDOWN, cooldown) : stack.without(DataComponents.USE_COOLDOWN);
        return stack;
    }

    private EnderPearlItem() {
        super(Material.ENDER_PEARL, RIGHT_CLICK_ANY);
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();

        var entity = EnderPearlEntity.shootFromPlayerDirection(player, true);
        ((MapPlayer2) player).addOwnedEntity(entity);

        if (isFinite(click.itemStack())) {
            click.consume(1);
        }
    }
}
