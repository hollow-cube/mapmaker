package net.hollowcube.mapmaker.test;

import net.hollowcube.ipc.map.MapData;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.PlayerState;
import net.minestom.server.entity.Player;

/// Minimum implementation of AbstractMapWorld for testing
public final class TestMapWorld extends AbstractMapWorld<TestMapWorld.State, TestMapWorld> {

    public TestMapWorld() {
        this(MapData.draft(java.util.UUID.randomUUID(), java.util.UUID.randomUUID()));
    }

    public TestMapWorld(MapData map) {
        super(null, map, makeMapInstance(map, 't'), State.class);
    }

    @Override
    protected State configurePlayer(Player player) {
        return new State.Active();
    }

    public sealed interface State extends PlayerState<State, TestMapWorld> {
        record Active() implements State {}
    }
}
