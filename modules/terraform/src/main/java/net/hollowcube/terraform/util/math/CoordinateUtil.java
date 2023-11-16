package net.hollowcube.terraform.util.math;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

public final class CoordinateUtil {
    private CoordinateUtil() {
    }

    public static @NotNull Point floor(@NotNull Point point) {
        return new Vec(point.blockX(), point.blockY(), point.blockZ());
    }

    public static @NotNull Point min(@NotNull Point a, @NotNull Point b) {
        return new Vec(
                Math.min(a.x(), b.x()),
                Math.min(a.y(), b.y()),
                Math.min(a.z(), b.z())
        );
    }

    public static @NotNull Point max(@NotNull Point a, @NotNull Point b) {
        return new Vec(
                Math.max(a.x(), b.x()),
                Math.max(a.y(), b.y()),
                Math.max(a.z(), b.z())
        );
    }

    public static @NotNull NBTCompound toNBT(@NotNull Point pos) {
        var nbt = new MutableNBTCompound();
        nbt.setDouble("x", pos.x());
        nbt.setDouble("y", pos.y());
        nbt.setDouble("z", pos.z());
        if (pos instanceof Pos p) {
            nbt.setFloat("yaw", p.yaw());
            nbt.setFloat("pitch", p.pitch());
        }
        return nbt.toCompound();
    }

    public static @NotNull Point fromNBT(@NotNull NBTCompound nbt) {
        var x = nbt.getDouble("x");
        var y = nbt.getDouble("y");
        var z = nbt.getDouble("z");
        if (nbt.contains("yaw") && nbt.contains("pitch")) {
            var yaw = nbt.getFloat("yaw");
            var pitch = nbt.getFloat("pitch");
            return new Pos(x, y, z, yaw, pitch);
        } else {
            return new Vec(x, y, z);
        }
    }

}
