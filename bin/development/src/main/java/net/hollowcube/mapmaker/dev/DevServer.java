package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class DevServer {

    public static void main(String[] args) throws Exception {

        MapServerInitializer.run(DevServerRunner::new, args);

//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/modules/hub/src/main/resources/spawn/hcspawn.polar");
//        var world = PolarReader.read(Files.readAllBytes(path));
//
//        var newWorld = new PolarWorld(
//                world.version(),
//                world.dataVersion(),
//                world.compression(),
//                world.minSection(), world.maxSection(),
//                world.userData(),
//                new ArrayList<>()
//        );
//
//        int total = 0;
//        outer:
//        for (var chunk : world.chunks()) {
//            for (var section : chunk.sections()) {
//                boolean isEmpty = section.isEmpty() || (section.blockPalette().length == 1 && NamespaceID.from(section.blockPalette()[0]).asMinimalString().equals("air"));
//                if (!isEmpty) {
//                    newWorld.updateChunkAt(chunk.x(), chunk.z(), chunk);
//                    total++;
//                    continue outer;
//                }
//            }
//
//            System.out.println("EMPTY SECTION AT " + chunk.x() + ", " + chunk.z());
//
//
//        }
//        System.out.println(total);
//
//        var newBytes = PolarWriter.write(newWorld);
//        System.out.println("total size: " + newBytes.length + " old is " + Files.readAllBytes(path).length);
//        Files.write(path, newBytes);
    }
}
