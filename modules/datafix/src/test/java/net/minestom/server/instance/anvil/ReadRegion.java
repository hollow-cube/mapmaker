package net.minestom.server.instance.anvil;

import net.kyori.adventure.nbt.TagStringIOExt;

import java.nio.file.Path;
import java.util.Objects;

public class ReadRegion {

    public static void main(String[] args) throws Exception {
        var path = "/Users/matt/Downloads/Wentworth Mansion/region/r.0.0.mca";
        var rf = new RegionFile(Path.of(path));
        var level = Objects.requireNonNull(rf.readChunkData(0, 0))
                .getCompound("Level");
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                var chunk = rf.readChunkData(x, z);
                if (chunk == null) continue;
                var entities = chunk.getCompound("Level").getList("Entities");
                for (var entity : entities) {

                    System.out.println(TagStringIOExt.writeTag(entity));
                }

            }
        }
    }
}
