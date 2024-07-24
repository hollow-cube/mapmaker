package net.hollowcube.mapmaker.map.script.engine;

import net.hollowcube.luau.util.Pin;
import org.jetbrains.annotations.NotNull;

public interface ScriptContainer {

    @NotNull Pin<?> getParent();

    void close();

}
