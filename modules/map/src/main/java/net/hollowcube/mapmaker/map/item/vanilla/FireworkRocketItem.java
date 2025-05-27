package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.play.vanilla.ElytraFeatureProvider;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FireworkRocketItem extends VanillaItemHandler {
    private static final Tag<Entity> FIREWORK_TAG = Tag.Transient("mapmaker:elytra_firework");
    private static final Tag<Integer> DURATION_TAG = Tag.Integer("firework_duration").defaultValue(0);

    public static final FireworkRocketItem INSTANCE = new FireworkRocketItem();

    public static void removeRocket(@NotNull Player player) {
        var removed = player.getAndSetTag(FIREWORK_TAG, null);
        if (removed != null) removed.remove();
    }

    public static @NotNull ItemStack get(int count, int durationMillis) {
        var stack = FireworkRocketItem.INSTANCE.getItemStack(count);
        stack = stack.withTag(DURATION_TAG, durationMillis);
        stack = stack.withLore(
                Component.text("Duration: " + NumberUtil.formatDuration(durationMillis))
                        .color(TextColor.color(0xB0B0B0))
                        .decoration(TextDecoration.ITALIC, false)
        );
        return stack;
    }

    private FireworkRocketItem() {
        super(Material.FIREWORK_ROCKET, RIGHT_CLICK_ANY);
    }

    @Override
    public void build(ItemStack.@NotNull Builder builder, @Nullable CompoundBinaryTag tag) {
        super.build(builder, tag);

        builder.material(this.item);
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
        if (isFinite(click.itemStack())) {
            click.consume(1);
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
