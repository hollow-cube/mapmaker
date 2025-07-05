package net.hollowcube.multipart.entity;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public record Transform(
        float dx, float dy, float dz,
        float rx, float ry, float rz,
        float sx, float sy, float sz,
        @NotNull Vec pivot
) {

    public Transform(@NotNull Vec position, @NotNull Vec rotation, @NotNull Vec scale, @NotNull Vec pivot) {
        this((float) position.x(), (float) position.y(), (float) position.z(),
                (float) rotation.x(), (float) rotation.y(), (float) rotation.z(),
                (float) scale.x(), (float) scale.y(), (float) scale.z(), pivot);
    }

    public static class Mutable {
        public float dx, dy, dz;
    }
}
