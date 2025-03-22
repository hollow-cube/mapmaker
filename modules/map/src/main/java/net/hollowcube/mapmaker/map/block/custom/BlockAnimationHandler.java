package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.EffectPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class BlockAnimationHandler extends ObjectEntityHandler {

    public static final String ID = "mapmaker:block_animation";
    private static final Tag<Set<String>> SHOWN = Tag.<Set<String>>Transient("mapmaker:block_animation/shown")
            .defaultValue(new HashSet<>());

    private static final long RATE = 5L * MinecraftServer.TICK_MS;
    private static final int MAX_BLOCKS = 128;
    private static final Block EMPTY = Block.AIR;

    private final Set<Player> viewers = new HashSet<>();

    private long duration;
    private long on;

    private boolean enabled = false;

    public BlockAnimationHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);

        onDataChange(null);
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.on = (entity.getData().getLong("on", 0) / RATE) * RATE;
        this.duration = this.on + (entity.getData().getLong("off", 0) / RATE) * RATE;

        this.enabled = this.entity.getBoundingBox().width() * this.entity.getBoundingBox().height() * this.entity.getBoundingBox().depth() <= MAX_BLOCKS;

        if (!this.enabled && player != null) {
            player.sendMessage(Component.translatable("handler.block_animation.too_big"));
        }
    }

    @Override
    public void onTick() {
        if (!this.enabled) return;

        var points = entity.getBoundingBox().contract(0.01, 0.01, 0.01).getBlocks(entity.getPosition());
        var instance = entity.getInstance();
        var id = this.entity.getUuid().toString();

        for (Player viewer : this.viewers) {
            if (viewer.getInstance() == null) continue;
            var world = MapWorld.forPlayerOptional(viewer);
            if (world == null) continue;

            var blocks = GhostBlockHolder.forPlayer(viewer);
            var playtime = viewer.getAliveTicks() * MinecraftServer.TICK_MS;
            var show = world.canEdit(viewer) || playtime % this.duration < this.on;

            if (viewer.getTag(SHOWN).contains(id) == show) continue;

            viewer.sendPacket(new BundlePacket());

            points.forEachRemaining(point -> {
                var pos = new Vec(point.blockX(), point.blockY(), point.blockZ());
                var block = instance.getBlock(pos, Block.Getter.Condition.TYPE);
                blocks.setBlock(pos, show ? block : EMPTY);
                if (block != null && !show) {
                    viewer.sendPacket(new EffectPacket(2001, pos, block.stateId(), false));
                }
            });

            viewer.sendPacket(new BundlePacket());

            viewer.updateTag(SHOWN, set -> {
                if (show) set.add(id);
                else set.remove(id);
                return set;
            });
        }
    }

    @Override
    public void addViewer(@NotNull MapWorld world, @NotNull Player player) {
        this.viewers.add(player);
    }

    @Override
    public void removeViewer(@NotNull MapWorld world, @NotNull Player player) {
        this.viewers.remove(player);
    }
}
