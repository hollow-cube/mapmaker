package net.hollowcube.terraform.task.edit;

import net.hollowcube.terraform.task.Task;
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

    static @NotNull WorldView snapshot(@NotNull Task task, @NotNull Instance instance) {
        return new WorldViewInstanceSnapshot(task, SnapshotUpdater.update(instance), instance.getWorldBorder());
    }

    static @NotNull WorldView instance(@NotNull Task task, @NotNull Instance instance) {
        return new WorldViewInstance(task, instance);
    }

    /**
     * Returns the task this world view was created with.
     */
    @NotNull Task task();

    boolean contains(int x, int y, int z);

}
