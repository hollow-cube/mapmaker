package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.scripting.api.*;
import net.hollowcube.mapmaker.scripting.require.ResourceRequireResolver;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WorldScriptContext {
    private static final Tag<ScriptContext.Player> PLAYER_SCRIPT_CONTEXT = Tag.Transient("hub/scripting-thread-ref");

    private static final LuauCompiler LUAU_COMPILER = LuauCompiler.builder()
        .userdataTypes() // todo
        .vectorType("vector")
        .vectorCtor("vec")
        .build();

    private final LuaState state;

    public WorldScriptContext(URI baseUrl) {
        this.state = LuaState.newState();
        state.callbacks().userThread(WorldScriptContext::onThreadChange);
        state.openLibs();

        state.openRequire(new ResourceRequireResolver(LUAU_COMPILER, baseUrl));
        registerGeneratedStringAtoms(state);

        LuaGlobals.register(state);
        LuaVector.register(state);
        LuaText$luau.register(state);
        LibItem.registerSlotGlobal(state);

        LibBase$luau.register(state);
        LibTask$luau.register(state);
        LibEnv$luau.register(state);
        LibPlayer$luau.register(state);
        LibEntity$luau.register(state);
        LibItem$luau.register(state);

        state.sandbox();
    }

    //todo move elsewhere
    private static final class PlayerContextImpl implements ScriptContext.Player {
        private final MapPlayer player;
        private final TagHandler tagHandler;
        private final List<Disposable> disposables;

        private final long lastPurge = System.currentTimeMillis();

        private PlayerContextImpl(
            MapPlayer player, TagHandler tagHandler,
            List<Disposable> disposables
        ) {
            this.player = player;
            this.tagHandler = tagHandler;
            this.disposables = disposables;
        }

        @Override
        public void track(Disposable disposable) {
            disposables.add(disposable);

            // probably a better time to do this idk, dnc for now
            if (lastPurge + 10_000 < System.currentTimeMillis()) {
                disposables.removeIf(Disposable::isDisposed);
            }
        }

        @Override
        public Scheduler scheduler() {
            return player.scheduler();
        }

        @Override
        public EventNode<Event> eventNode() {
            System.out.println("returning event node for " + player.getUsername());
            // Obviously unsafe, not sure a better option immediately.
            //noinspection unchecked
            return (EventNode<Event>) (Object) player.eventNode();
        }

        @Override
        public MapPlayer player() {
            return player;
        }

        @Override
        public TagHandler tagHandler() {
            return tagHandler;
        }

        public List<Disposable> disposables() {
            return disposables;
        }
    }

    public void initializePlayer(MapPlayer player) {
        var thread = state.newThread();
        var context = new PlayerContextImpl(player, TagHandler.newHandler(), new ArrayList<>());
        player.setTag(PLAYER_SCRIPT_CONTEXT, context);
        thread.setThreadData(context);

        // we dont need to keep a ref to the main thread since we have the player script context.
        // The context will hold any references we need to clean up when destroying the player.
        state.pop(1); // remove thread from main thread stack

        thread.sandboxThread(); // Create mutable env for script usage

        var playerScript = Objects.requireNonNull(HubMapWorld.class.getResource("/scripts/player.luau"));
        try (var is = playerScript.openStream()) {
            var source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            var bytecode = LUAU_COMPILER.compile(source);
            thread.load("/player.luau", bytecode);
            thread.call(0, 0);
        } catch (Exception e) {
            player.kick("Failed to spawn player");
            throw new RuntimeException("failed to spawn player", e);
        }
    }

    public void destroyPlayer(MapPlayer player) {
        var context = player.getAndSetTag(PLAYER_SCRIPT_CONTEXT, null);
        if (context == null) throw new IllegalStateException("Player has no thread reference");

        for (var disposable : ((PlayerContextImpl) context).disposables()) {
            if (disposable.isDisposed()) continue;
            disposable.dispose();
        }
    }

    private static void onThreadChange(@Nullable LuaState parent, LuaState thread) {
        if (parent == null) return; // Destruction, dont care for now.

        // If our parent has a thread data, copy its context to our thread.
        // This will _not_ happen for any top level threads which is intentional
        // because we set up the script context manually for those ones.
        var parentData = parent.getThreadData();
        if (parentData instanceof ThreadData td)
            thread.setThreadData(td.scriptContext());
    }

    private static void registerGeneratedStringAtoms(LuaState state) {
        // todo cache this or something
        try {
            Class.forName("net.hollowcube.luau.gen.runtime.GeneratedStringAtoms")
                .getDeclaredMethod("register", LuaState.class)
                .invoke(null, state);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
