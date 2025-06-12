package net.minestom.server.instance.anvil;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public class RegionFileWrapper {
    private final RegionFile regionFile;

    public RegionFileWrapper(Path path) throws IOException {
        this.regionFile = new RegionFile(path);
    }

    public @Nullable CompoundBinaryTag readChunkData(int chunkX, int chunkZ) throws IOException {
        return regionFile.readChunkData(chunkX, chunkZ);
    }
}
