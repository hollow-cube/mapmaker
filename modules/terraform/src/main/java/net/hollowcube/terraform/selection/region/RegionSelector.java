package net.hollowcube.terraform.selection.region;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

public interface RegionSelector {

    /**
     * Adds/sets a primary selection point to the current region.
     *
     * @param explain If true, the change in selection will be "explained" to the player.
     *                If not updated, no explanation will be sent to the player.
     *                The CUI rendering will be updated regardless of this option.
     * @return True if the point was added, false if it was not (nothing was changed)
     */
    boolean selectPrimary(@NotNull Point point, boolean explain);

    /**
     * Adds/sets a secondary selection point to the current region.
     *
     * @param explain If true, the change in selection will be "explained" to the player.
     *                If not updated, no explanation will be sent to the player.
     *                The CUI rendering will be updated regardless of this option.
     * @return True if the point was added, false if it was not (nothing was changed)
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

    /**
     * Changes the size of the region by the specified amount.
     *
     * @param delta            The amount to change the size of the region by. If the number is negative, it will shrink, if it is positive, the region will grow
     * @param changeVertical   If the size change should modify the selection in the vertical direction (+/- y coordinate). True to modify, false to not
     * @param changeHorizontal If the size change should modify the selection in the horizontal directions (+/- x and z coordinates). True to modify, false to not
     */
    @Deprecated //todo would rather explicit expand/contract methods which operate on xyz
    default void changeSize(int delta, boolean changeVertical, boolean changeHorizontal) {
        throw new UnsupportedOperationException();
    }

    void write(@NotNull NetworkBuffer buffer);

    void read(@NotNull NetworkBuffer buffer);

}
