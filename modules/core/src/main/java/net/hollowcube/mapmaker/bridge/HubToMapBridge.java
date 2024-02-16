package net.hollowcube.mapmaker.bridge;

import net.hollowcube.map.runtime.ServerBridge;

/**
 * Implements required communication from hub servers to all available map servers.
 * <p>
 * This represents the only form of direct communication from hub servers to maps. The implementation may
 * communicate in any way. For example the dev server uses a singleton class as a bridge within the same process
 * however an alternative implementation may use a proxy, a REST API, asynchronous messaging, etc.
 */
public interface HubToMapBridge extends ServerBridge {

}
