package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.cui.ClientRenderer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public interface Region extends Iterable<@NotNull Point> {

    enum Type {
        CUBOID(CuboidRegionSelector::new),
        LINE(LineRegionSelector::new),
        BEZIER_SURFACE(BezierSurfaceRegionSelector::new);

        private final BiFunction<ClientInterface, String, RegionSelector> factory;

        Type(@NotNull BiFunction<ClientInterface, String, RegionSelector> factory) {
            this.factory = factory;
        }

        public @NotNull RegionSelector newSelector(@NotNull ClientInterface cui, @NotNull String selectionId) {
            return factory.apply(cui, selectionId);
        }
    }

    /**
     * Minimum rectangular bounding point (inclusive)
     */
    @NotNull Point min();

    /**
     * Maximum rectangular bounding point (exclusive)
     */
    @NotNull Point max();

    default int volume() {
        var min = min();
        var max = max();
        return (max.blockX() - min.blockX()) * (max.blockY() - min.blockY()) * (max.blockZ() - min.blockZ());
    }

}
