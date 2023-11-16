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

    //todo expand, contract on xyz individually.

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
