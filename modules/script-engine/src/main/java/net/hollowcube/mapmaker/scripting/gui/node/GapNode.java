package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import org.jetbrains.annotations.NotNull;

public class GapNode extends Node {

    public GapNode() {
        super("gap");
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        // Noop!
    }
}
