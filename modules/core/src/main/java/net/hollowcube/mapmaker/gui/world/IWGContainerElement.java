package net.hollowcube.mapmaker.gui.world;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class IWGContainerElement extends IWGElement {
    private final List<IWGElement> children = new ArrayList<>();

    public @NotNull IWGContainerElement addChild(@NotNull IWGElement child) {
        children.add(child);
        return this;
    }

    public @NotNull List<IWGElement> children() {
        return children;
    }
}
