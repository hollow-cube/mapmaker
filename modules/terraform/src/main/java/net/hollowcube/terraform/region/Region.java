package net.hollowcube.terraform.region;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

/**
 * Represents any area within a world
 * <p>
 * Region implementations should be immutable.
 */
public interface Region extends Iterable<@NotNull Point> {

    //todo instance

    /** Minimum rectangular bounding point */
    @NotNull Point min();
    /** Maximum rectangular bounding point */
    @NotNull Point max();

    default int volume() {
        var min = min();
        var max = max();
        return (max.blockX() - min.blockX() + 1) * (max.blockY() - min.blockY() + 1) * (max.blockZ() - min.blockZ() + 1);
    }

}
