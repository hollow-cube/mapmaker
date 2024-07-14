package net.hollowcube.mapmaker.map.script.engine;

import net.hollowcube.luau.util.Pin;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.script.object.LuaWorld;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public class WorldScriptContainer implements ScriptContainer {

    private final Pin<LuaWorld> worldRef;
    private final EventNode<InstanceEvent> eventNode;

    public WorldScriptContainer(@NotNull MapWorld world, @NotNull EventNode<InstanceEvent> eventNode) {
        this.worldRef = Pin.value(new LuaWorld(world));
        this.eventNode = eventNode;
    }

    @Override
    public void addListener(EventListener<?> listener) {
        eventNode.addListener((EventListener<? extends InstanceEvent>) listener);
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
