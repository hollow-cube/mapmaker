package net.hollowcube.mapmaker.gui.world;

import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class InWorldGui implements Viewable {
    private final Set<Player> viewers = new HashSet<>();
    protected IWGElement root = null;

    private float rotation = 0;

    private Instance instance;
    private Point position;

    public void setInstance(@NotNull Instance instance, @NotNull Point position) {
        this.instance = instance;
        this.position = position;

        root.setInstance(instance, position);
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        if (!viewers.add(player))
            return false;

        //todo
        return true;
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        if (!viewers.remove(player))
            return false;

        //todo
        return true;
    }

    @Override
    public @NotNull Set<@NotNull Player> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }
}
