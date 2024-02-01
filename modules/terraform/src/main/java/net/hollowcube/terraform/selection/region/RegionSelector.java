package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.cui.ClientInterface;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public interface RegionSelector {

    record Factory(@NotNull String id, @NotNull Function<ClientInterface, RegionSelector> factory) {
    }

    /**
     * Adds/sets a primary selection pos to the current region.
     *
     * @param explain If true, the change in selection will be "explained" to the player.
     *                If not updated, no explanation will be sent to the player.
     *                The CUI rendering will be updated regardless of this option.
     * @return True if the pos was added, false if it was not (nothing was changed)
     */
    boolean selectPrimary(@NotNull Point point, boolean explain);

    /**
     * Adds/sets a secondary selection pos to the current region.
     *
     * @param explain If true, the change in selection will be "explained" to the player.
     *                If not updated, no explanation will be sent to the player.
     *                The CUI rendering will be updated regardless of this option.
     * @return True if the pos was added, false if it was not (nothing was changed)
     */
    boolean selectSecondary(@NotNull Point point, boolean explain);

    /**
     * Completely wipes the selection, updates the rendering but does not send any message.
     */
    void clear();

    /**
     * Returns the current region, or null if the selection is incomplete.
     */
    @Nullable Region region();

    // Modification

    /**
     * Reshapes the region by shifting the "low" and "high" points by the given amounts. The "low" and "high" points
     * are subject to the implementation of the region, and reshape may not be supported at all in some cases.
     *
     * <p>Example 1: to shift the selection over by 10 on the X you would pass 10,0,0 and 10,0,0.</p>
     * <p>Example 2: to uniformly expand by 5 you would pass -5,-5,-5 and 5,5,5.</p>
     *
     * @param low  The shift amount for the "low" points
     * @param high The shift amount for the "high" points
     * @throws UnsupportedOperationException If the region does not support reshaping
     */
    default void reshape(@NotNull Point low, @NotNull Point high) {
        throw new UnsupportedOperationException("Region does not support reshaping");
    }

    // Serialization

    /**
     * Write this region selector data to the given {@link NetworkBuffer}.
     *
     * <p>Implementations are responsible for handling format changes internally (eg using a version number).</p>
     */
    void write(@NotNull NetworkBuffer buffer);

    /**
     * Read this region selector data from the given {@link NetworkBuffer}.
     *
     * <p>Implementations are responsible for handling format changes internally (eg using a version number).</p>
     */
    void read(@NotNull NetworkBuffer buffer);

}
