package net.hollowcube.mapmaker.runtime.hub.feature;

import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.runtime.freeform.FreeformMapWorld;
import org.jetbrains.annotations.NotNull;

/**
 * Loaded with SPI
 */
public interface HubFeature {

    void load(@NotNull MapServer server, @NotNull FreeformMapWorld world);

}
