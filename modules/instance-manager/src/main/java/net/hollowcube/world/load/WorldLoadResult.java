package net.hollowcube.world.load;

import net.hollowcube.api.ITemporaryWorld;

/**
 * This class contains the WorldLoadResultCode and the world itself when loading has finished.
 * The world object may be null if something went wrong. Always check the WorldLoadResultCode first.
 */
public class WorldLoadResult {
    private final ITemporaryWorld world;
    private final WorldLoadResultCode worldLoadResultCode;

    public WorldLoadResult(ITemporaryWorld world, WorldLoadResultCode worldLoadResultCode) {
        this.world = world;
        this.worldLoadResultCode = worldLoadResultCode;
    }

    /**
     * @return Returns the loaded world object. May be null if something went wrong.
     */
    public ITemporaryWorld getWorld() {
        return this.world;
    }

    /**
     * @return Returns the WorldLoadResultCode.
     */
    public WorldLoadResultCode getWorldLoadResultCode() {
        return this.worldLoadResultCode;
    }
}
