package net.hollowcube.mapmaker.map.script.engine;

import net.hollowcube.luau.BuilinLibrary;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.util.PinImpl;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.script.api.EventImpl$Wrapper;
import net.hollowcube.mapmaker.map.script.api.LuaEventSource$Wrapper;
import net.hollowcube.mapmaker.map.script.api.LuaSystem$Wrapper;
import net.hollowcube.mapmaker.map.script.api.entity.LuaEntity$Wrapper;
import net.hollowcube.mapmaker.map.script.api.entity.LuaMarkerEntity$Wrapper;
import net.hollowcube.mapmaker.map.script.api.entity.LuaPlayer$Wrapper;
import net.hollowcube.mapmaker.map.script.api.item.ItemStackTypeImpl;
import net.hollowcube.mapmaker.map.script.api.math.LuaCuboid;
import net.hollowcube.mapmaker.map.script.api.math.VectorTypeImpl;
import net.hollowcube.mapmaker.map.script.api.world.BlockTypeImpl;
import net.hollowcube.mapmaker.map.script.api.world.LuaWorld$Wrapper;
import net.hollowcube.mapmaker.map.script.api.world.LuaWorldView$Wrapper;
import net.hollowcube.mapmaker.map.script.loader.MapScriptLoader;
import net.hollowcube.mapmaker.map.script.loader.ScriptManifest;
import net.kyori.adventure.text.Component;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Exists per map, manages the lua environment.
 */
public class ScriptEngine {
    private static final Logger logger = LoggerFactory.getLogger(ScriptEngine.class);
    public static final Tag<ScriptEngine> TAG = Tag.Transient("script_engine");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("script-events", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handlePlayerJoined)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::handlePlayerLeave);

    private final MapWorld world;
    private final MapScriptLoader loader;

    private final ScriptManifest manifest;

    private final LuaState global;
    private final Map<UUID, Map.Entry<LuaState, Integer>> threads = new HashMap<>();

    public ScriptEngine(@NotNull MapWorld world, @NotNull MapScriptLoader loader) {
        this.world = world;
        this.loader = loader;

        this.global = LuaState.newState();
        loadGlobals();

        this.manifest = loader.getManifest();
        for (var script : manifest.entries()) {
            if (script.type() != ScriptManifest.Type.WORLD) continue;
            logger.info("Loading world script: {}", script.filename());
            loadScript(script.id(), script.filename(), UUID.randomUUID(), new WorldScriptContainer(world, eventNode));
        }

        world.instance().eventNode().addChild(eventNode);
    }

    public @Nullable LuaState getThread(@NotNull UUID threadId) {
        var pair = threads.get(threadId);
        return pair == null ? null : pair.getKey();
    }

    public void sendDebugLog(@NotNull String message) {
        Audiences.all().sendMessage(Component.text("[lua] ").append(Component.text(message)));
    }

    public void close() {
        world.instance().eventNode().removeChild(eventNode);

        threads.values().forEach(thread -> {
            ((ScriptContainer) thread.getKey().getThreadData()).close();
            global.unref(thread.getValue());
        });
        threads.clear();
        global.close();
    }

    private void handlePlayerJoined(@NotNull MapPlayerInitEvent event) {
        if (!event.isFirstInit()) return;

        for (var script : manifest.entries()) {
            if (script.type() != ScriptManifest.Type.PLAYER) continue;
            logger.info("Loading player script: {}", script.filename());
            loadScript(script.id(), script.filename(), event.player().getUuid(),
                    new PlayerScriptContainer(eventNode, event.player()));
        }
    }

    private void handlePlayerLeave(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var thread = threads.remove(event.getPlayer().getUuid());
        if (thread == null) return;
        ((ScriptContainer) thread.getKey().getThreadData()).close();
        global.unref(thread.getValue());
    }

    private void loadScript(@NotNull String id, @NotNull String filename, @NotNull UUID threadId, @NotNull ScriptContainer parent) {
        try {
            byte[] bytecode = loader.getScriptBytecode(id);

            LuaState thread = global.newThread();
            threads.put(threadId, Map.entry(thread, global.ref(-1)));
            global.pop(1); // Remove the thread from the stack (it will remain as long as ref does)

            thread.sandboxThread();
            thread.setThreadData(parent);

            thread.load(filename, bytecode);
            thread.pcall(0, 0); // eval the code.
        } catch (RuntimeException e) {
            if (e.getCause() instanceof LuauCompileException lce) {
                sendDebugLog("Compilation failed for " + filename);
                sendDebugLog(lce.getMessage());
            } else {
                sendDebugLog(e.getMessage());
            }
        }
    }

    private void loadGlobals() {
        global.openLibs(BuilinLibrary.BASE, BuilinLibrary.MATH);
        global.pushCFunction(this::luaPrint, "luaPrint");
        global.setGlobal("print");

        VectorTypeImpl.init(global);
        BlockTypeImpl.init(global);
        ItemStackTypeImpl.init(global);

        LuaEntity$Wrapper.initMetatable(global);
        LuaPlayer$Wrapper.initMetatable(global);
        LuaWorldView$Wrapper.initMetatable(global);
        LuaWorld$Wrapper.initMetatable(global);
        EventImpl$Wrapper.initMetatable(global); // todo: bad name
        LuaEventSource$Wrapper.initMetatable(global);
//        TriggerImpl$Wrapper.initMetatable(global); // todo: bad name
        LuaSystem$Wrapper.initMetatable(global);
        LuaCuboid.init(global);
        LuaMarkerEntity$Wrapper.initMetatable(global);

        // Create metatable for `script`, which has an __index into java.
        global.newMetaTable("script");
        global.pushString("__index");
        global.pushCFunction((state) -> {
            state.checkType(1, LuaType.TABLE);
            var key = state.checkStringArg(2);

            if ("Parent".equals(key) && state.getThreadData() instanceof ScriptContainer psc) {
                ((PinImpl<?>) psc.getParent()).push(state);
                return 1;
            }

            state.error("No such key: " + key);
            return 0;
        }, "scriptIndex");
        global.setTable(-3);

        // Create a global table with a metatable of `script`.
        global.newTable();
        global.getMetaTable("script");
        global.setMetaTable(-2);
        global.setReadOnly(-2, true);
        global.setGlobal("script");

        // Create a global state for System

        global.sandbox();
    }

    // Tracks very closely to luaB_print, but doesnt go to stdout.
    private int luaPrint(@NotNull LuaState state) {
        var message = new StringBuilder();

        int nArgs = state.getTop();
        for (int i = 1; i <= nArgs; i++) {
            var msg = state.toString(i);
            if (i > 1) message.append("\t");
            message.append(msg);
            state.pop(1);
        }

        sendDebugLog(message.toString());
        return 0;
    }

}
