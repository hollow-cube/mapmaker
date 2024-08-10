package net.hollowcube.mapmaker.map.script.api.world;

import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.script.AbstractRefManager;
import net.hollowcube.mapmaker.map.script.ScriptEngine;
import net.hollowcube.mapmaker.map.script.api.LuaEventSource;
import net.hollowcube.mapmaker.map.script.api.entity.LuaEntity;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * <p>A view of the world from the perspective of the player from which it was obtained.</p>
 *
 * <p>Because this is a view for the specific player, the world state may appear from a different WorldView, or
 * the global World. Actions taken (blocks/entities modified) will occur for ONLY the specific player which this
 * view represents.</p>
 */
@LuaObject
public class LuaWorldView {

    private final Player player;
    private final MapWorld world;

    private LuaEventSource<Callbacks.OnTick> onTick;

    public LuaWorldView(@NotNull Player player) {
        this.player = player;
        this.world = MapWorld.forPlayer(player);
    }

    @LuaProperty
    public @NotNull String getName() {
        return world.worldId();
    }

    @LuaProperty
    public @NotNull LuaEventSource<Callbacks.OnTick> getOnTick() {
        if (onTick == null) {
            onTick = LuaEventSource.create(
                    Callbacks.OnTick.class, PlayerTickEvent.class,
                    (_, onTick) -> onTick.call()
            );
        }
        return onTick;
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
    public @NotNull LuaEntity spawnEntity(@NotNull String entityTypeName, @NotNull Point pos) {
        var entityType = EntityType.fromNamespaceId(entityTypeName);
        if (entityType == null) throw new RuntimeException("No such entity: " + entityTypeName);

        var entity = MapEntityType.create(entityType, UUID.randomUUID());
        entity.setAutoViewable(false);
        entity.setInstance(world.instance(), pos).thenRun(() -> entity.addViewer(player));

        refManager().addEntity(entity);
        return new LuaEntity(entity);
    }

    private @NotNull AbstractRefManager refManager() {
        var scriptEngine = world.instance().getTag(ScriptEngine.TAG);
        Check.notNull(scriptEngine, "ScriptEngine not found in world");
        var playerThread = scriptEngine.getThread(player.getUuid());
        Check.notNull(playerThread, "Player thread not found");
        return (AbstractRefManager) Objects.requireNonNull(playerThread.getThreadData());
    }

    public static final class Callbacks {

        @LuaBindable
        public interface OnTick {
            void call();
        }

    }

}
