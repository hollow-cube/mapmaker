package net.hollowcube.terraform.region.selector;

import net.hollowcube.terraform.region.CuboidRegion;
import net.hollowcube.terraform.region.Region;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CuboidRegionSelector implements RegionSelector {
    private Instance instance = null;
    private Point pos1 = null, pos2 = null;

    @Override
    public @Nullable Instance getInstance() {
        return instance;
    }

    @Override
    public void setInstance(@NotNull Instance instance) {
        this.instance = instance;
    }

    @Override
    public boolean selectPrimary(@NotNull Point point) {
        if (pos1 != null && point.sameBlock(pos1)) return false;
        pos1 = point;
        return true;
    }

    @Override
    public boolean selectSecondary(@NotNull Point point) {
        if (pos2 != null && point.sameBlock(pos2)) return false;
        pos2 = point;
        return true;
    }

    @Override
    public void clear() {
        instance = null;
        pos1 = null;
        pos2 = null;
    }

    @Override
    public @Nullable Region getRegion() {
        if (instance == null || pos1 == null || pos2 == null) return null;
        return new CuboidRegion(instance, pos1, pos2);
    }
}
