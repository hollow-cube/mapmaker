package net.hollowcube.world;

import net.hollowcube.world.storage.FileStorage;
import org.jetbrains.annotations.NotNull;

public class WorldManager {

    private final FileStorage fileStorage;

    public WorldManager(@NotNull FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public @NotNull FileStorage fileStorage() {
        return fileStorage;
    }

}
