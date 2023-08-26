package net.hollowcube.map.block.handler;

import net.hollowcube.map.MapHooks;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractPlateHandler implements BlockHandler {
    private static final BoundingBox BOUNDING_BOX = new BoundingBox(14.0 / 16.0, 1.0 / 16.0, 14.0 / 16.0);

    @Override
    public boolean isTickable() {
        return true;
    }

    @Override
    public void tick(@NotNull Tick tick) {
        var instance = tick.getInstance();
        var pos = tick.getBlockPosition();
        var centerPos = new Vec(pos.blockX() + 0.5, pos.blockY(), pos.blockZ() + 0.5);

        // Check for collision with all players in instance
        var entities = instance.getNearbyEntities(pos, 2);
        for (var entity : entities) {
            Player player;
            if (entity instanceof Player p) {
                if (!MapHooks.isPlayerPlaying(p)) continue;
                player = p;
            } else if (entity.hasTag(MapHooks.ASSOCIATED_PLAYER)) {
                player = entity.getTag(MapHooks.ASSOCIATED_PLAYER);
            } else continue;

            if (!entity.getBoundingBox().intersectBox(centerPos.sub(entity.getPosition()), BOUNDING_BOX))
                continue;

            // Player has stepped on the plate
            onPlatePressed(tick, player);
        }
    }

    public abstract void onPlatePressed(@NotNull Tick tick, @NotNull Player player);
}
