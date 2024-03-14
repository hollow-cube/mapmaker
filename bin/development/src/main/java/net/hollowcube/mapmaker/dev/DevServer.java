package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

public class DevServer {
    public static void main(String[] args) throws Exception {

//        var data = Files.readAllBytes(Path.of("/Users/matt/Downloads/d2afd6d9-b3a7-4799-bc00-b75ff9acc00d"));
//
//        var world = PolarReader.read(data);
//
//        System.out.println(world);

//        var loader = new PolarLoader(new ByteArrayInputStream(worldData));
//        if (worldAccess != null) loader.setWorldAccess(worldAccess);

        MapServerInitializer.run(DevServerRunner::new, args);
    }
}
