package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.luau.util.Pinned;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaWorldView implements Pinned {

    private final Player player;
    private final MapWorld world;

    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.OnTick>> onTick;
    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.OnBlockInteract>> onBlockInteract;

    public LuaWorldView(@NotNull Player player) {
        this.player = player;
        this.world = MapWorld.forPlayer(player);

        this.onTick = Pin.value(LuaEventSource.create(
                PlayerTickEvent.class, Callbacks.OnTick.class,
                (_, onTick) -> onTick.call()
        ));
        this.onBlockInteract = Pin.value(LuaEventSource.create(
                PlayerBlockInteractEvent.class, Callbacks.OnBlockInteract.class,
                (e, onBlockInteract) -> onBlockInteract.call(e.getBlockPosition(), e.getBlock())
        ));
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

    //        todo spawnEntity
    //        var entityName = state.checkStringArg(2);
    //        var pos = state.checkVectorArg(3);
    //        state.checkType(4, LuaType.TABLE);
    //
    //        var entityType = EntityType.fromNamespaceId(entityName);
    //        if (entityType == null) {
    //            state.error("No such entity: " + entityName);
    //            return 0;
    //        }
    //        var entity = MapEntityType.create(entityType, UUID.randomUUID());
    //        playerEntities.add(entity);
    //        entity.setAutoViewable(false);
    //        entity.setInstance(world.instance(), new Vec(pos[0], pos[1], pos[2]))
    //                .thenRun(() -> entity.addViewer(player));
    //
    //        state.newTable();
    //        return 1;


    @Override
    public void unpin() {
        onTick.close();
        onBlockInteract.close();
    }

    public static final class Callbacks {

        @LuaBindable
        public interface OnTick {
            void call();
        }

        @LuaBindable
        public interface OnBlockInteract {
            void call(@NotNull Point pos, @NotNull Block block);
        }

    }
}
