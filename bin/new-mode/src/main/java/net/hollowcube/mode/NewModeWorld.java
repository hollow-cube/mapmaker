package net.hollowcube.mode;

import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import org.jetbrains.annotations.NotNull;

public class NewModeWorld extends AbstractMapWorld {
    public static final Constructor<NewModeWorld> CTOR = AbstractMapWorld.ctor(NewModeWorld::new, NewModeWorld.class);

    protected NewModeWorld(@NotNull MapServer server, @NotNull MapData map) {
        super(server, map, new MapInstance("mapmaker:test", false));

        instance().setGenerator(MapGenerators.stoneWorld());
    }

    @Override
    public void load() {

    }

}
