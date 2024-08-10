package net.hollowcube.mapmaker.map.script.container;

import net.hollowcube.luau.util.Pin;
import net.hollowcube.mapmaker.map.MapWorld;
import org.jetbrains.annotations.NotNull;

public interface ScriptContainer {

    @NotNull MapWorld world();

    @NotNull Pin<?> getParent();

    void close();

}
