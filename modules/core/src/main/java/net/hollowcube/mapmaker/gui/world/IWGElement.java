package net.hollowcube.mapmaker.gui.world;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public abstract class IWGElement {
    public abstract @NotNull Point boundingBox();

    public abstract void setInstance(@NotNull Instance instance, @NotNull Point position);
}
