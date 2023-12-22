package net.hollowcube.mapmaker.hub.feature;

import net.hollowcube.mapmaker.hub.HubServer;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * Loaded with SPI
 */
public interface HubFeature {

    @Blocking
    void init(@NotNull HubServer hub);

}
