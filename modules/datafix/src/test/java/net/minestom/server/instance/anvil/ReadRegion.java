package net.minestom.server.instance.anvil;

import net.kyori.adventure.nbt.TagStringIOExt;

import java.nio.file.Path;

public class ReadRegion {

    public static void main(String[] args) throws Exception {
        var path = "/Users/matt/Downloads/BUGGY WUGGY/region/r.0.0.mca";
        var rf = new RegionFile(Path.of(path));
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                if (x != 3 || z != 3) continue;
                var chunk = rf.readChunkData(x, z);
                if (chunk == null) continue;
                var level = chunk.getCompound("Level");
//                System.out.println(level.keySet());
                var entities = level.getList("Entities");
                for (var entity : entities) {
                    System.out.println("ENT: " + TagStringIOExt.writeTag(entity));
                }
                var tileEntities = level.getList("TileEntities");
                for (var tileEntity : tileEntities) {
                    System.out.println("TILE: " + TagStringIOExt.writeTag(tileEntity));
                }

            }
        }
    }
}
