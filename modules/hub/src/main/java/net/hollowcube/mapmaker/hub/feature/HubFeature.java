package net.hollowcube.mapmaker.hub.feature;

import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.map.MapServer;
import org.jetbrains.annotations.NotNull;

/**
 * Loaded with SPI
 */
public interface HubFeature {

    void load(@NotNull MapServer server, @NotNull HubMapWorld world);

}
