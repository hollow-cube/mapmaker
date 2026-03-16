package net.hollowcube.mapmaker.hub.feature;

import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.map.MapServer;

/**
 * Loaded with SPI
 */
public interface HubFeature {

    void load(MapServer server, HubMapWorld world);

}
