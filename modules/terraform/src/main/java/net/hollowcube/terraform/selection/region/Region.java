package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.cui.ClientInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.BiFunction;

public interface Region extends Iterable<@NotNull Point> {

    enum Type implements ComponentLike {
        CUBOID(CuboidRegionSelector::new),
        LINE(LineRegionSelector::new),
        BEZIER_SURFACE(BezierSurfaceRegionSelector::new);

        public static final NetworkBuffer.Type<Type> NETWORK_TYPE = NetworkBuffer.Enum(Type.class);

        private final BiFunction<ClientInterface, String, RegionSelector> factory;

        Type(@NotNull BiFunction<ClientInterface, String, RegionSelector> factory) {
            this.factory = factory;
        }

        public @NotNull RegionSelector newSelector(@NotNull ClientInterface cui, @NotNull String selectionId) {
            return factory.apply(cui, selectionId);
        }

        @Override
        public @NotNull Component asComponent() {
            return Component.translatable("terraform.region." + name().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Minimum rectangular bounding pos (inclusive)
     */
    @NotNull Point min();

    /**
     * Maximum rectangular bounding pos (exclusive)
     */
    @NotNull Point max();

    default @NotNull Point size() {
        return max().sub(min());
    }

    default int volume() {
        var min = min();
        var max = max();
        return (max.blockX() - min.blockX()) * (max.blockY() - min.blockY()) * (max.blockZ() - min.blockZ());
    }

}
