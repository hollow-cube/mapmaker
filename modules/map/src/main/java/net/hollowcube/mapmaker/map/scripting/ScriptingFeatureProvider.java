package net.hollowcube.mapmaker.map.scripting;

import com.google.auto.service.AutoService;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.scripting.api.LuaEventSource;
import net.hollowcube.mapmaker.map.scripting.api.math.LuaVectorTypeImpl;
import net.hollowcube.mapmaker.map.scripting.api.player.LuaPlayer;
import net.hollowcube.mapmaker.map.scripting.api.world.LuaParticle;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@AutoService(FeatureProvider.class)
public class ScriptingFeatureProvider implements FeatureProvider {
    private static final String QB_DG = "5ede93ae-493a-4318-963e-7fa6ac7fc30b";

    private static final Tag<LuaState> LUA_STATE_TAG = Tag.Transient("mapmaker:play/lua_state");

    public record ThreadRef(int ref, EventNode eventNode) {
    }

    public static final Tag<ThreadRef> LUA_THREAD_REF_TAG = Tag.Transient("mapmaker:play/lua_thread_ref");

    private static final LuauCompiler COMPILER = LuauCompiler.builder()
            .userdataTypes() // todo
            .vectorType("vector")
            .vectorCtor("vec")
            .build();

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/scripting", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::deinitPlayer);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!QB_DG.equals(world.map().id())) return false;
        if (!(world instanceof PlayingMapWorld) && !(world instanceof TestingMapWorld))
            return false;

        world.eventNode().addChild(eventNode);
        var global = createGlobalState();
        world.setTag(LUA_STATE_TAG, global);

        return true;
    }

    @Override
    public void cleanupMap(@NotNull MapWorld world) {
        var global = world.getAndSetTag(LUA_STATE_TAG, null);
        if (global != null) global.close();
    }

    private void initPlayer(@NotNull MapPlayerInitEvent event) {
        var global = Objects.requireNonNull(event.getMapWorld().getTag(LUA_STATE_TAG));

        var state = global.newThread();
        state.sandboxThread();

        final var player = event.player();
        LuaPlayer.push(state, new LuaPlayer(player));
        state.setGlobal("player");

        EventNode eventNode = EventNode.type("script-local", EventFilter.ENTITY);
        event.getMapWorld().eventNode().addChild(eventNode);

        // Create a persistent reference to the table
        int ref = global.ref(-1);
        player.setTag(LUA_THREAD_REF_TAG, new ThreadRef(ref, eventNode));

        try {
            var bytecode = COMPILER.compile("""
                    
                    local function onCheckpointChanged()
                        player:SpawnParticle(Particle.EndRod, player.Position + vec(0, 1, 0), vec(0, 0, 0), 0.3, 30)
                    
                        player:PlaySound("minecraft:ui.loom.take_result", player.Position, 1.5, 0.6)
                        player:PlaySound("minecraft:entity.cat.ambient", player.Position, 1.5, 0.9)
                        player:PlaySound("minecraft:entity.cat.purr", player.Position, 2, 1)
                    
                        -- TODO: Do text animation
                    
                        player:SendMessage("<pride>Checkpoint changed!")
                    end
                    
                    player.CheckpointChanged:Listen(onCheckpointChanged)
                    """);

            state.load("test.luau", bytecode);
            state.pcall(0, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deinitPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var global = Objects.requireNonNull(event.getMapWorld().getTag(LUA_STATE_TAG));

        var player = event.getPlayer();
        var ref = Objects.requireNonNull(player.getAndSetTag(LUA_THREAD_REF_TAG, null));
        event.getMapWorld().eventNode().removeChild(ref.eventNode);

        global.unref(ref.ref);
    }

    private @NotNull LuaState createGlobalState() {
        var global = LuaState.newState();
        global.openLibs(); // todo probably dont give all for now

        LuaVectorTypeImpl.init(global);
        LuaEventSource.init(global);
        LuaParticle.init(global);
        LuaPlayer.init(global);

        global.sandbox();
        return global;
    }
}
