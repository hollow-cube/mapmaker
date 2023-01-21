package net.hollowcube.terraform.selection.region;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RegionSelector {

    boolean selectPrimary(@NotNull Point point);
    void explainPrimary(@NotNull Point point);

    boolean selectSecondary(@NotNull Point point);
    void explainSecondary(@NotNull Point point);

    void clear();

    @Nullable Region region();

}
