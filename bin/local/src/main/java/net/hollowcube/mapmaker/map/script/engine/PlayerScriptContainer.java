package net.hollowcube.mapmaker.map.script.engine;

import net.hollowcube.luau.util.Pin;
import net.hollowcube.mapmaker.map.script.object.LuaPlayer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerScriptContainer implements ScriptContainer {

    private final Pin<LuaPlayer> playerRef;
    private final EventNode<InstanceEvent> eventNode;

    public PlayerScriptContainer(@NotNull Player player, @NotNull EventNode<InstanceEvent> eventNode) {
        this.playerRef = Pin.value(new LuaPlayer(player));
        this.eventNode = eventNode;
    }

    @Override
    public void addListener(EventListener<?> listener) {
        eventNode.addListener((EventListener<? extends InstanceEvent>) listener);
    }

    @Override
    public @NotNull Pin<?> getParent() {
        return this.playerRef;
    }

    @Override
    public void close() {
        this.playerRef.close();
    }

}
