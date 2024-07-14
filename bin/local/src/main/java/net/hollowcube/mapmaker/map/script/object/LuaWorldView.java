package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.luau.util.Pinned;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@LuaObject
public class LuaWorldView implements Pinned {

    private final Player player;
    private final MapWorld world;

    private final List<Pin<LuaEntity>> playerEntities = new ArrayList<>();

    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.OnTick>> onTick;
    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.OnBlockInteract>> onBlockInteract;

    public LuaWorldView(@NotNull Player player) {
        this.player = player;
        this.world = MapWorld.forPlayer(player);

        this.onTick = LuaEventSource.create(
                Callbacks.OnTick.class, PlayerTickEvent.class,
                (_, onTick) -> onTick.call()
        );
        this.onBlockInteract = LuaEventSource.create(
                Callbacks.OnBlockInteract.class, PlayerBlockInteractEvent.class,
                (e, onBlockInteract) -> onBlockInteract.call(e.getBlockPosition(), e.getBlock())
        );
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

    @LuaMethod
    public @NotNull Pin<LuaEntity> spawnEntity(@NotNull String entityTypeName, @NotNull Point pos) {
        var entityType = EntityType.fromNamespaceId(entityTypeName);
        if (entityType == null) throw new RuntimeException("No such entity: " + entityTypeName);

        var entity = MapEntityType.create(entityType, UUID.randomUUID());
        entity.setAutoViewable(false);
        entity.setInstance(world.instance(), pos).thenRun(() -> entity.addViewer(player));

        var pin = Pin.value(new LuaEntity(entity));
        playerEntities.add(pin);
        return pin;
    }

    @Override
    public void unpin() {
        onTick.close();
        onBlockInteract.close();

        playerEntities.forEach(Pin::close);
        playerEntities.clear();
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
