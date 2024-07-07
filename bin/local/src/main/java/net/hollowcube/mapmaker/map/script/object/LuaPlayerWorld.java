package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.script.friendly.LuaObject;
import net.hollowcube.mapmaker.map.script.friendly.Ref;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LuaPlayerWorld implements LuaObject {
    private static final String TYPE_NAME = LuaPlayerWorld.class.getName();

    public static void initGlobalRef(@NotNull LuaState global) {
        global.newMetaTable(TYPE_NAME);

        global.pushCFunction(LuaPlayerWorld::luaIndex, "__index");
        global.setField(-2, "__index");

        global.pushCFunction(LuaPlayerWorld::luaNameCall, "__namecall");
        global.setField(-2, "__namecall");

        global.pop(1); // Pop the metatable
    }

    private static int luaIndex(@NotNull LuaState state) {
        LuaPlayerWorld ref = (LuaPlayerWorld) state.checkUserDataArg(1, TYPE_NAME);
        String key = state.checkStringArg(2);

        return switch (key) {
            case "Name" -> {
                state.pushString(ref.getName());
                yield 1;
            }
            case "OnTick" -> {
                ref.getOnTick().push(state);
                yield 1;
            }
            case "OnBlockInteract" -> {
                ref.getOnBlockInteract().push(state);
                yield 1;
            }
            default -> {
                state.argError(2, "No such key: " + key);
                yield 0; // Never reached
            }
        };
    }

    private static int luaNameCall(@NotNull LuaState state) {
        LuaPlayerWorld ref = (LuaPlayerWorld) state.checkUserDataArg(1, TYPE_NAME);
        String method = state.nameCallAtom();

        return switch (method) {
            case "GetBlock" -> ref.getBlock(state);
            case "SetBlock" -> ref.setBlock(state);
            case "SpawnEntity" -> ref.spawnEntity(state);
            default -> {
                state.error("No such method: " + state.checkStringArg(2));
                yield 0;
            }
        };
    }

    private final LuaState state;
    private final Player player;
    private final MapWorld world;

    private final EventNode<? extends InstanceEvent> eventNode;
    private final Ref<EventSource<PlayerTickEvent>> onTickRef;
    private final Ref<EventSource<PlayerBlockInteractEvent>> onBlockInteractRef;

    public LuaPlayerWorld(@NotNull LuaState state, @NotNull Player player, @NotNull MapWorld world) {
        this.state = state;
        this.player = player;
        this.world = world;

        this.eventNode = EventNode.event("player-world-events", EventFilter.INSTANCE, event -> {
            if (!(event instanceof PlayerInstanceEvent pie)) return false;
            return pie.getPlayer() == player && pie.getInstance() == world.instance();
        });
        this.world.instance().eventNode().addChild(eventNode);

        this.onTickRef = new Ref<>(state, new EventSource<>(state,
                (EventNode<PlayerTickEvent>) eventNode,
                PlayerTickEvent.class));
        this.onBlockInteractRef = new Ref<>(state, new EventSource<>(state,
                (EventNode<PlayerBlockInteractEvent>) eventNode,
                PlayerBlockInteractEvent.class,
                (s, event) -> {
                    var blockPos = event.getBlockPosition();
                    s.pushVector((float) blockPos.x(), (float) blockPos.y(), (float) blockPos.z());
                    BlockType.pushBlock(s, event.getBlock());
                    return 2;
                }, 1, (s, event) -> event.setCancelled(s.toBoolean(1))));
    }

    public @NotNull String getName() {
        return world.worldId();
    }

    public @NotNull Ref<EventSource<PlayerTickEvent>> getOnTick() {
        return onTickRef;
    }

    public @NotNull Ref<EventSource<PlayerBlockInteractEvent>> getOnBlockInteract() {
        return onBlockInteractRef;
    }

    public int getBlock(@NotNull LuaState state) {
        var pos = state.checkVectorArg(2);

        var ghostBlocks = GhostBlockHolder.forPlayer(player);
        var blockState = ghostBlocks.getBlock(new Vec(pos[0], pos[1], pos[2]), Block.Getter.Condition.TYPE);

        BlockType.pushBlock(state, blockState);
        return 1;
    }

    public int setBlock(@NotNull LuaState state) {
        var pos = state.checkVectorArg(2);
        var block = Block.fromStateId((int) state.checkUserDataArg(3, BlockType.TYPE_NAME));

        var ghostBlocks = GhostBlockHolder.forPlayer(player);
        ghostBlocks.setBlock(new Vec(pos[0], pos[1], pos[2]), block);

        return 0;
    }

    private final List<Entity> playerEntities = new ArrayList<>();

    public int spawnEntity(@NotNull LuaState state) {
        var entityName = state.checkStringArg(2);
        var pos = state.checkVectorArg(3);
        state.checkType(4, LuaType.TABLE);

        var entityType = EntityType.fromNamespaceId(entityName);
        if (entityType == null) {
            state.error("No such entity: " + entityName);
            return 0;
        }
        var entity = MapEntityType.create(entityType, UUID.randomUUID());
        playerEntities.add(entity);
        entity.setAutoViewable(false);
        entity.setInstance(world.instance(), new Vec(pos[0], pos[1], pos[2]))
                .thenRun(() -> entity.addViewer(player));

        state.newTable();
        return 1;
    }

    public void close(@NotNull LuaState state) {
        world.instance().eventNode().removeChild(eventNode);

        onTickRef.close(state);
        onBlockInteractRef.close(state);

        playerEntities.forEach(Entity::remove);
        playerEntities.clear();

    }

}
