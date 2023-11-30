package net.hollowcube.map.feature.play.checkpoint;

import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.object.ObjectData;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CheckpointCache {
    private static final Logger logger = LoggerFactory.getLogger(CheckpointCache.class);

    private static final Tag<CheckpointCache> TAG = Tag.Transient("mapmaker:checkpoint/cache");

    public static @NotNull CheckpointCache forInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(instance.getTag(TAG));
    }

    private final Map<String, ObjectData> checkpoints = new HashMap<>();

    public CheckpointCache(@NotNull MapWorld world) {
        for (var object : world.map().objects()) {
            if (object.type() == CheckpointPlateBlock.OBJECT_TYPE) {
                checkpoints.put(object.id(), object);
            }
        }

        world.instance().setTag(TAG, this);
    }

    public boolean isEmpty() {
        return checkpoints.isEmpty();
    }

    public @UnknownNullability ObjectData getCheckpoint(@NotNull String id) {
        return checkpoints.get(id);
    }

}
