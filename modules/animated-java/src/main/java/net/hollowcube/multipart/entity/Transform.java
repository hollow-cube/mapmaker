package net.hollowcube.multipart.entity;

import net.hollowcube.aj.util.Quaternion;
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

    public static final class Mutable {
        public Vec translation;
        public Quaternion rotation;

        public Mutable() {
            this.translation = Vec.ZERO;
            this.rotation = new Quaternion(0, 0, 0, 1);
        }

        public Mutable(Vec translation, Quaternion rotation) {
            this.translation = translation;
            this.rotation = rotation;
        }

        public Mutable copy() {
            return new Mutable(translation, rotation);
        }
    }
}
