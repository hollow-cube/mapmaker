package net.hollowcube.map.feature.checkpoint;

import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.object.ObjectData;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CheckpointCache {
    private static final Tag<CheckpointCache> TAG = Tag.Transient("mapmaker:checkpoint/cache");

    public static @NotNull CheckpointCache forInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(instance.getTag(TAG));
    }

    private final Map<String, ObjectData> checkpoints = new HashMap<>();

    public CheckpointCache(@NotNull MapWorld world) {
        for (var object : world.map().objects()) {
            if (object.type() == CheckpointPlateBlock.OBJECT_TYPE) {
                checkpoints.put(object.id(), object);
                System.out.println("Adding checkpoint to world: " + object.toString());
            }
        }

        world.instance().setTag(TAG, this);
    }

    public boolean isEmpty() {
        return checkpoints.isEmpty();
    }

}
