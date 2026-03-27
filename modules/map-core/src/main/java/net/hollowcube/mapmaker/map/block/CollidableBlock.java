package net.hollowcube.mapmaker.map.block;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNullByDefault;

/// A block handler which can receive collision triggers from a [MapPlayer].
///
/// Collision is tested relative to the center of the bounding box. The [BoundingBox] constructor will
/// create an appropriate bounding box for this purpose.
///
/// **Note:** The [#collisionBox()] may not extend outside of a single block bounding box. This is a
/// limitation of the current collision logic. It could be lifted in the future if necessary.
@NotNullByDefault
public interface CollidableBlock extends BlockHandler {

    record Collision(MapWorld world, Player player, BlockVec blockPosition, Block block) {
    }

    BoundingBox collisionBox();

    default void onEnter(Collision collision) {
    }

    default void onExit(Collision collision) {
    }

}
