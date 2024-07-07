package net.hollowcube.mapmaker.map.script.object2;

import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaWorldView {

    private final Player player;
    private final MapWorld world;

    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.OnTick>> onTick;
    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.OnBlockInteract>> onBlockInteract;

    public LuaWorldView(@NotNull Player player) {
        this.player = player;
        this.world = MapWorld.forPlayer(player);

        this.onTick = null; //todo
        this.onBlockInteract = null; //todo
    }

    @LuaProperty
    public @NotNull String getName() {
        return world.worldId();
    }

    @LuaMethod
    public @NotNull Block getBlock(@NotNull Point pos) {
        return GhostBlockHolder.forPlayer(player).getBlock(pos);
    }

    @LuaMethod
    public void setBlock(@NotNull Point pos, @NotNull Block block) {
        GhostBlockHolder.forPlayer(player).setBlock(pos, block);
    }

    //todo spawnEntity

    public static final class Callbacks {

        public interface OnTick {
            void call();
        }

        public interface OnBlockInteract {
            void call(@NotNull Point pos, @NotNull Block block);
        }

    }
}
