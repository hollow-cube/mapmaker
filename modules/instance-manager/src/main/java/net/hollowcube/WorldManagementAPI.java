package net.hollowcube;

import net.hollowcube.api.ITemporaryWorld;
import net.hollowcube.api.IWorld;
import net.hollowcube.api.IWorldManagementAPI;
import net.hollowcube.world.load.WorldLoadResult;
import net.hollowcube.world.unload.WorldSaveResult;

import java.util.function.Consumer;

public class WorldManagementAPI implements IWorldManagementAPI {
    private final WorldManager worldManager;

    public WorldManagementAPI(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public void loadWorld(String id) {
        this.loadWorld(id, null);
    }

    @Override
    public void loadWorld(String id, Consumer<WorldLoadResult> consumer) {
        //TODO call world loader
    }

    @Override
    public void unloadWorld(String id) {
        this.unloadWorld(id, null);
    }

    @Override
    public void unloadWorld(ITemporaryWorld world) {
        this.unloadWorld(world, null);
    }

    @Override
    public void unloadWorld(String id, Runnable runnable) {
        //TODO convert id to actual world then call this.unloadWorld(world, runnable)
    }

    @Override
    public void unloadWorld(ITemporaryWorld world, Runnable runnable) {
        world.unloadWorld(runnable);
    }

    @Override
    public void saveWorld(String id) {
        this.saveWorld(id, null);
    }

    @Override
    public void saveWorld(IWorld world) {
        this.saveWorld(world, null);
    }

    @Override
    public void saveWorld(String id, Consumer<WorldSaveResult> consumer) {
        //TODO convert id to actual world then call this.saveWorld(world, consumer)
    }

    @Override
    public void saveWorld(IWorld world, Consumer<WorldSaveResult> consumer) {
        world.saveWorld(consumer);
    }

    @Override
    public void saveAndUnloadWorld(String id) {
        this.saveAndUnloadWorld(id, null);
    }

    @Override
    public void saveAndUnloadWorld(IWorld world) {
        this.saveAndUnloadWorld(world, null);
    }

    @Override
    public void saveAndUnloadWorld(String id, Consumer<WorldSaveResult> consumer) {
        //TODO convert id to actual world then call this.unloadAndSaveWorld(world, consumer)
    }

    @Override
    public void saveAndUnloadWorld(IWorld world, Consumer<WorldSaveResult> consumer) {
        world.saveAndUnloadWorld(consumer);
    }

    @Override
    public WorldManager getWorldManager() {
        return this.worldManager;
    }
}
