package net.hollowcube.mapmaker.map.script.engine;

import net.hollowcube.luau.util.Pin;
import net.minestom.server.event.EventListener;
import org.jetbrains.annotations.NotNull;

public interface ScriptContainer {

    @NotNull Pin<?> getParent();

    void addListener(EventListener<?> listener);

    void close();

}
