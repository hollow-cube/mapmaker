package net.hollowcube.mapmaker.scripting.gui;

import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public abstract class Node {

    public boolean updateFromProps(@NotNull Value props) {
        return false;
    }
}
