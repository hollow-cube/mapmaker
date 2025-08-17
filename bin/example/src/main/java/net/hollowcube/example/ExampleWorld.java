package net.hollowcube.example;

import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.minestom.server.entity.Player;

public class ExampleWorld extends AbstractMapWorld<ExampleState, ExampleWorld> {

    protected ExampleWorld(MapServer server, MapData map) {
        super(server, map, makeMapInstance(map, 'e'), ExampleState.class);

        instance().setGenerator(MapGenerators.stoneWorld());
    }

    @Override
    protected ExampleState configurePlayer(Player player) {
        return new ExampleState.Main();
    }

}
