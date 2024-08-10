package net.hollowcube.mapmaker.map.script.container;

import net.hollowcube.luau.util.Pin;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.script.AbstractRefManager;
import net.hollowcube.mapmaker.map.script.api.world.LuaWorld;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public class WorldScriptContainer extends AbstractRefManager implements ScriptContainer {

    private final MapWorld world;
    private final Pin<LuaWorld> worldRef;

    public WorldScriptContainer(@NotNull MapWorld world, @NotNull EventNode<InstanceEvent> eventNode) {
        super(eventNode, _ -> true);

        this.world = world;

        this.worldRef = Pin.value(new LuaWorld(world));
    }

    @Override
    public @NotNull MapWorld world() {
        return world;
    }

    @Override
    public @NotNull Pin<?> getParent() {
        return this.worldRef;
    }

    @Override
    public void close() {
        this.worldRef.close();
    }
}
