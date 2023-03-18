package net.hollowcube.terraform.action.edit;

import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.snapshot.SnapshotUpdater;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable view of a "world".
 * <p>
 * The backing source may or may not actually be a world.
 */
public interface WorldView extends Block.Getter {

    static @NotNull WorldView snapshot(@NotNull Instance instance) {
        return new WorldViewInstanceSnapshot(SnapshotUpdater.update(instance));
    }

    static @NotNull WorldView instance(@NotNull Instance instance) {
        return new WorldViewInstance(instance);
    }

}
