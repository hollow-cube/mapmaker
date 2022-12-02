package net.hollowcube.world.unload;

import net.hollowcube.api.ITemporaryWorld;

/**
 * This class contains the WorldSaveResultCode and the world itself when saving has finished.
 * The world object may be null if something went wrong. Always check the WorldSaveResultCode first.
 */
public class WorldSaveResult {
    private final ITemporaryWorld world;
    private final WorldSaveResultCode worldSaveResultCode;

    public WorldSaveResult(ITemporaryWorld world, WorldSaveResultCode worldSaveResultCode) {
        this.world = world;
        this.worldSaveResultCode = worldSaveResultCode;
    }

    /**
     * @return Returns the saved world object. May be null if something went wrong.
     */
    public ITemporaryWorld getWorld() {
        return this.world;
    }

    /**
     * @return Returns the WorldSaveResultCode.
     */
    public WorldSaveResultCode getWorldSaveResultCode() {
        return this.worldSaveResultCode;
    }
}
