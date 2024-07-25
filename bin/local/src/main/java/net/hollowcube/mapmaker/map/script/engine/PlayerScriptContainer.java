package net.hollowcube.mapmaker.map.script.engine;

import net.hollowcube.luau.util.Pin;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.script.api.entity.LuaPlayer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerScriptContainer extends AbstractRefManager implements ScriptContainer {

    private final MapWorld world;

    private final Pin<LuaPlayer> playerRef;

    public PlayerScriptContainer(@NotNull EventNode<InstanceEvent> parentNode, @NotNull Player player) {
        super(parentNode, event -> event instanceof PlayerInstanceEvent ev && ev.getPlayer() == player);

        this.world = MapWorld.forPlayer(player);
        this.playerRef = Pin.value(new LuaPlayer(player));
    }

    @Override
    public @NotNull MapWorld world() {
        return world;
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
