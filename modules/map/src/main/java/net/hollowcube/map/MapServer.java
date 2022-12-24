package net.hollowcube.map;

import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.world.WorldManager;
import org.jetbrains.annotations.NotNull;

public interface MapServer {

    @NotNull WorldManager worldManager();

    @NotNull MapStorage mapStorage();

    @NotNull SaveStateStorage saveStateStorage();

}
