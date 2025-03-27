package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.play.vanilla.ElytraFeatureProvider;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FireworkRocketItem extends ItemHandler {
    private static final Tag<Entity> FIREWORK_TAG = Tag.Transient("mapmaker:elytra_firework");
    private static final Tag<Integer> DURATION_TAG = Tag.Integer("firework_duration").defaultValue(0);
    private static final String INFINITE_MODEL = "mapmaker:infinite_firework";

    public static final FireworkRocketItem INSTANCE = new FireworkRocketItem();
    public static final ItemStack DEFAULT = setDurationMillis(
            ItemStack.of(Material.FIREWORK_ROCKET)
                    .without(DataComponents.FIREWORKS)
                    .with(DataComponents.MAX_STACK_SIZE, 99),
            1000);

    public static void removeRocket(@NotNull Player player) {
        var removed = player.getAndSetTag(FIREWORK_TAG, null);
        if (removed != null) removed.remove();
    }

    public static int getDurationMillis(@NotNull ItemStack itemStack) {
        return itemStack.getTag(DURATION_TAG);
    }

    public static @NotNull ItemStack setDurationMillis(@NotNull ItemStack itemStack, int durationMillis) {
        return itemStack.withTag(DURATION_TAG, durationMillis)
                .with(DataComponents.LORE, List.of(
                        Component.text("Duration: " + NumberUtil.formatDuration(durationMillis), TextColor.color(0xB0B0B0))
                                .decoration(TextDecoration.ITALIC, false)));
    }

    public static boolean isInfinite(@NotNull ItemStack itemStack) {
        return INFINITE_MODEL.equals(itemStack.get(DataComponents.ITEM_MODEL));
    }

    public static int getCount(@NotNull ItemStack itemStack) {
        return isInfinite(itemStack) ? 0 : itemStack.amount();
    }

    public static @NotNull ItemStack withCount(@NotNull ItemStack itemStack, int count) {
        if (count <= 0) {
            return itemStack.withAmount(1).with(DataComponents.ITEM_MODEL, INFINITE_MODEL);
        } else {
            return itemStack.withAmount(count).without(DataComponents.ITEM_MODEL);
        }
    }

    private FireworkRocketItem() {
        super("minecraft:firework_rocket", RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.FIREWORK_ROCKET;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return; // Sanity

        // You can only start a rocket boost while already gliding
        if (!player.hasTag(ElytraFeatureProvider.IS_GLIDING_TAG)) return;

        int durationTicks = click.itemStack().getTag(DURATION_TAG) / MinecraftServer.TICK_MS;
        spawnRocketEntity(player, durationTicks);
        if (!isInfinite(click.itemStack())) {
            click.updateItemStack(b -> b.amount(click.itemStack().amount() - 1));
        }
    }

    private void spawnRocketEntity(@NotNull Player player, int durationTicks) {
        player.sendPacket(new BundlePacket()); // Bundle for the player only so there is no gap in rockets when its replaced.
        try {
            removeRocket(player);

            var newRocket = new FireworkRocketEntity(player, durationTicks);
            newRocket.setInstance(player.getInstance(), player.getPosition());
            player.setTag(FIREWORK_TAG, newRocket);
        } finally {
            player.sendPacket(new BundlePacket());
        }
    }

    private static class FireworkRocketEntity extends Entity {
        private static final ItemStack EMPTY_FIREWORK = ItemStack.of(Material.FIREWORK_ROCKET);

        private final Player ridingPlayer;
        private final int lifetime;

        public FireworkRocketEntity(@NotNull Player ridingPlayer, int lifetime) {
            super(EntityType.FIREWORK_ROCKET);
            this.ridingPlayer = ridingPlayer;
            this.lifetime = lifetime;

            setSynchronizationTicks(Long.MAX_VALUE);
            setNoGravity(true);
            hasPhysics = false;

            getEntityMeta().setShooter(ridingPlayer);
            getEntityMeta().setFireworkInfo(EMPTY_FIREWORK);

            this.updateViewableRule(other -> other == ridingPlayer || ridingPlayer.isViewer(other));
        }

        @Override
        public @NotNull FireworkRocketMeta getEntityMeta() {
            return (FireworkRocketMeta) super.getEntityMeta();
        }

        @Override
        protected void movementTick() {
            // Intentionally do nothing
        }

        @Override
        public void tick(long time) {
            super.tick(time);

            if (ridingPlayer.isRemoved()) {
                remove();
                return;
            }

            if (lifetime > 0 && lifetime < getAliveTicks()) {
                remove();
                return;
            }

            if (isRemoved() || instance == null) return;
            refreshPosition(ridingPlayer.getPosition());
        }
    }
}
