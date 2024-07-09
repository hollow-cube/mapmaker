package net.hollowcube.mapmaker.map.script;

import net.hollowcube.luau.BuilinLibrary;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.script.object.Impl$Wrapper;
import net.hollowcube.mapmaker.map.script.object.LuaPlayer$Wrapper;
import net.hollowcube.mapmaker.map.script.object.LuaSystem$Wrapper;
import net.hollowcube.mapmaker.map.script.object.LuaWorldView$Wrapper;
import net.hollowcube.mapmaker.map.script.type.BlockTypeImpl;
import net.hollowcube.mapmaker.map.script.type.VectorTypeImpl;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public class MapScriptContainer {
    public static final Tag<MapScriptContainer> TAG = Tag.Transient("map_script_container");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("script-events", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handlePlayerJoined)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::handlePlayerLeave);

    private final MapWorld world;
    private final Path workspace;

    private final LuaState global;

    public MapScriptContainer(@NotNull MapWorld world, @NotNull Path workspace) {
        this.world = world;
        this.workspace = workspace;

        this.global = LuaState.newState();
        loadGlobals();

        world.instance().eventNode().addChild(eventNode);
    }

    public void close() {
        global.close();
    }

    private void loadGlobals() {
        global.openLibs(BuilinLibrary.BASE, BuilinLibrary.MATH);
        global.pushCFunction(this::luaPrint, "luaPrint");
        global.setGlobal("print");

        VectorTypeImpl.init(global);
        BlockTypeImpl.init(global);

        LuaPlayer$Wrapper.initMetatable(global);
        LuaWorldView$Wrapper.initMetatable(global);
        Impl$Wrapper.initMetatable(global); // todo: bad name
        LuaSystem$Wrapper.initMetatable(global);

        // Create metatable for `script`, which has an __index into java.
        global.newMetaTable("script");
        global.pushString("__index");
        global.pushCFunction((state) -> {
            state.checkType(1, LuaType.TABLE);
            var key = state.checkStringArg(2);

            if ("Parent".equals(key) && state.getThreadData() instanceof PlayerScriptContainer psc) {
//                psc.getParent().push(state);
//                return 1;
                return 0;
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

        global.sandbox();
    }

    private void handlePlayerJoined(@NotNull MapPlayerInitEvent event) {
        try {
            var script = Files.readAllBytes(workspace.resolve("src/Player.luau"));
            var bytecode = LuauCompiler.DEFAULT.compile(new String(script)); //todo use byte array overload

            var playerScript = new PlayerScriptContainer(global, event.player());
            event.player().setTag(PlayerScriptContainer.TAG, playerScript);
            playerScript.eval("Player.luau", bytecode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handlePlayerLeave(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var playerScript = event.player().getTag(PlayerScriptContainer.TAG);
        event.player().removeTag(PlayerScriptContainer.TAG);
        if (playerScript != null) {
            playerScript.close();
        }
    }

    // Tracks very closely to luaB_print, but doesnt go to stdout.
    private int luaPrint(@NotNull LuaState state) {
        var message = new StringBuilder();
        message.append("[lua] ");

        int nArgs = state.getTop();
        for (int i = 1; i <= nArgs; i++) {
            var msg = state.toString(i);
            if (i > 1) message.append("\t");
            message.append(msg);
            state.pop(1);
        }

        world.instance().sendMessage(Component.text(message.toString()));
        return 0;
    }

}
