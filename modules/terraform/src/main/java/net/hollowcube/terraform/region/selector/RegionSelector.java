package net.hollowcube.terraform.region.selector;

import net.hollowcube.terraform.region.Region;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to select a region given primary and secondary points added.
 */
public interface RegionSelector {

    @Nullable Instance getInstance();

    void setInstance(@NotNull Instance instance);

    /**
     * Set/add a primary point to the selector.
     * @return true if the selection changed, false otherwise
     */
    boolean selectPrimary(@NotNull Point point);

    /**
     * Set/add a secondary point to the selector.
     * @return true if the selection changed, false otherwise
     */
    boolean selectSecondary(@NotNull Point point);

    /**
     * Clears the points and instance set in this selector.
     */
    void clear();

    /** Get the region selected, or null if it is incomplete */
    @Nullable Region getRegion();

}
