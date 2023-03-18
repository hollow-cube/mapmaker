package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.selection.cui.SelectionRenderer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public interface Region extends Iterable<@NotNull Point> {

    @SuppressWarnings("Immutable") // I dont know how to make BiFunction immutable... seems like it should be
    enum Type {
        CUBOID(CuboidRegionSelector::new),
        BEZIER_SURFACE(BezierSurfaceRegionSelector::new);

        private final BiFunction<Player, SelectionRenderer, RegionSelector> factory;

        Type(@NotNull BiFunction<Player, SelectionRenderer, RegionSelector> factory) {
            this.factory = factory;
        }

        public @NotNull RegionSelector newSelector(@NotNull Player player, @NotNull SelectionRenderer renderer) {
            return factory.apply(player, renderer);
        }
    }

    /** Minimum rectangular bounding point (inclusive) */
    @NotNull Point min();

    /** Maximum rectangular bounding point (exclusive) */
    @NotNull Point max();

    default int volume() {
        var min = min();
        var max = max();
        return (max.blockX() - min.blockX()) * (max.blockY() - min.blockY()) * (max.blockZ() - min.blockZ());
    }

}
