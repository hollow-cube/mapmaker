package net.hollowcube;

import net.hollowcube.api.IWorldManagementAPI;

public class WorldManagementAPIProvider {
    protected static IWorldManagementAPI STATIC_API;

    protected static void setAPI(WorldManagementAPI api) {
        WorldManagementAPIProvider.STATIC_API = api;
    }

    private final IWorldManagementAPI api;

    public WorldManagementAPIProvider() {
        this.api = STATIC_API;
    }

    public IWorldManagementAPI getAPI() {
        return this.api;
    }
}
