package net.hollowcube.example;

import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import org.jetbrains.annotations.NotNull;

public class ExampleWorld extends AbstractMapWorld {
    public static final Constructor<ExampleWorld> CTOR = AbstractMapWorld.ctor(ExampleWorld::new, ExampleWorld.class);

    protected ExampleWorld(@NotNull MapServer server, @NotNull MapData map) {
        super(server, map, new MapInstance("mapmaker:test", MapInstance.LightingMode.FULL_BRIGHT));

        instance().setGenerator(MapGenerators.stoneWorld());
    }

    @Override
    public void load() {

    }

}
