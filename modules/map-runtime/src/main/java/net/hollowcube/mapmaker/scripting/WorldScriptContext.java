package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.luau.require.RequireResolver;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.scripting.api.*;
import net.hollowcube.mapmaker.scripting.require.FsModuleLoader;
import net.hollowcube.mapmaker.scripting.require.ResourceRequireResolver;
import net.hollowcube.mapmaker.scripting.require.ZipRequireResolver;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.GenericTempActionBarProvider;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WorldScriptContext {
    private static final Tag<ScriptContext.Player> PLAYER_SCRIPT_CONTEXT = Tag.Transient("hub/scripting-thread-ref");
    private static final Tag<ScriptContext.World> WORLD_SCRIPT_CONTEXT = Tag.Transient("hub/scripting-thread-ref-world");

    private static final LuauCompiler LUAU_COMPILER = LuauCompiler.builder()
        .userdataTypes() // todo
        .vectorType("vector")
        .vectorCtor("vec")
        .build();
    private static final Logger logger = LoggerFactory.getLogger(WorldScriptContext.class);

    private final MapWorld world;

    private final LuaState state;
    private final @Nullable FsModuleLoader fsModuleLoader;
    private final @Nullable Map<String, byte[]> vfs;
    private final @Nullable URI baseUrl;

    public WorldScriptContext(MapWorld world, Path scriptDirectory) {
        this.world = world;
        try {
            this.fsModuleLoader = new FsModuleLoader(LUAU_COMPILER, scriptDirectory, this::onReload);
            this.state = newStateWithGlobals(fsModuleLoader);
            fsModuleLoader.globalState = this.state;
            this.vfs = null;
            this.baseUrl = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public WorldScriptContext(MapWorld world, URI baseUrlOrZip, boolean isZip) {
        this.world = world;
        try {
            this.fsModuleLoader = null;
            if (isZip) {
                var resolver = new ZipRequireResolver(LUAU_COMPILER, baseUrlOrZip);
                this.vfs = resolver.getVfsThisIsBadPleaseFix();
                this.state = newStateWithGlobals(resolver);
                this.baseUrl = null;
            } else {
                this.vfs = null;
                this.state = newStateWithGlobals(new ResourceRequireResolver(LUAU_COMPILER, baseUrlOrZip));
                this.baseUrl = baseUrlOrZip;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static LuaState newStateWithGlobals(RequireResolver resolver) {
        var state = LuaState.newState();
        state.callbacks().userThread(WorldScriptContext::onThreadChange);
        state.openLibs();

        state.openRequire(resolver);
        registerGeneratedStringAtoms(state);

        LuaGlobals.register(state);
        LuaVector.register(state);
        LuaRuntime$luau.register(state);
        LuaText$luau.register(state);
        LibItem.registerSlotGlobal(state);

        LibBase$luau.register(state);
        LibTask$luau.register(state);
        LibEnv$luau.register(state);
        LibWorld$luau.register(state);
        LibPlayer$luau.register(state);
        LibPlayers$luau.register(state);
        LibEntity$luau.register(state);
        LibItem$luau.register(state);
        LibStore$luau.register(state);

        state.sandbox();
        return state;
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

    private static final class WorldContextImpl implements ScriptContext.World {
        private final MapWorld world;
        private final TagHandler tagHandler;
        private final List<Disposable> disposables;

        private final long lastPurge = System.currentTimeMillis();

        private WorldContextImpl(
            MapWorld world, TagHandler tagHandler,
            List<Disposable> disposables
        ) {
            this.world = world;
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
            return world.scheduler();
        }

        @Override
        public EventNode<Event> eventNode() {
            // Obviously unsafe, not sure a better option immediately.
            //noinspection unchecked
            return (EventNode<Event>) (Object) world.eventNode();
        }

        @Override
        public MapWorld world() {
            return world;
        }

        @Override
        public TagHandler tagHandler() {
            return tagHandler;
        }

        public List<Disposable> disposables() {
            return disposables;
        }
    }

    public void initializeWorld() {
        var context = new WorldContextImpl(world, TagHandler.newHandler(), new ArrayList<>());
        world.instance().setTag(WORLD_SCRIPT_CONTEXT, context);
        initWorldThread(context);
    }

    public void initializePlayer(MapPlayer player) {
        var context = new PlayerContextImpl(player, TagHandler.newHandler(), new ArrayList<>());
        player.setTag(PLAYER_SCRIPT_CONTEXT, context);
        initPlayerThread(context);
    }

    private void onReload(FsModuleLoader.ReloadEvent event) {
        var context = (WorldContextImpl) Objects.requireNonNull(world.instance().getTag(WORLD_SCRIPT_CONTEXT));
        context.disposables.removeIf(disposable -> {
            if (disposable.isDisposed()) return true;
            if (disposable.disposeOnReload() && disposable.chunkName() != null && event.invalidated().contains(disposable.chunkName())) {
                disposable.dispose();
                return true;
            }
            return false;
        });
        initWorldThread(context);

        var actionBar = new GenericTempActionBarProvider("File changed: " + event.changed(), 1000);
        for (var player : world.players()) {
            ActionBar.forPlayer(player).addProvider(actionBar);
        }

//        for (var player : world.players()) {
//            var context = (PlayerContextImpl) Objects.requireNonNull(player.getTag(PLAYER_SCRIPT_CONTEXT));
//            context.disposables.removeIf(disposable -> {
//                if (disposable.isDisposed()) return true;
//                if (disposable.disposeOnReload() && disposable.chunkName() != null && event.invalidated().contains(disposable.chunkName())) {
//                    disposable.dispose();
//                    return true;
//                }
//                return false;
//            });
//            initPlayerThread(context);
//
//            ActionBar.forPlayer(player).addProvider(new GenericTempActionBarProvider("File changed: " + event.changed(), 1000));
//        }
    }

    private void initWorldThread(WorldContextImpl context) {
        var thread = state.newThread();
        thread.setThreadData(context);

        // we dont need to keep a ref to the main thread since we have the player script context.
        // The context will hold any references we need to clean up when destroying the player.
        state.pop(1); // remove thread from main thread stack

        thread.sandboxThread(); // Create mutable env for script usage

        if (fsModuleLoader != null) {
            try {
                var bytecode = fsModuleLoader.readAndParseFile("/world");
                thread.load("/world", bytecode);
                thread.call(0, 0);
            } catch (Exception e) {
                throw new RuntimeException("failed to create world context", e);
            }
        }
    }

    private void initPlayerThread(PlayerContextImpl context) {
        var thread = state.newThread();
        thread.setThreadData(context);

        // we dont need to keep a ref to the main thread since we have the player script context.
        // The context will hold any references we need to clean up when destroying the player.
        state.pop(1); // remove thread from main thread stack

        thread.sandboxThread(); // Create mutable env for script usage

        if (fsModuleLoader != null) {
            try {
                var bytecode = fsModuleLoader.readAndParseFile("/player");
                thread.load("/player", bytecode);
                thread.call(0, 0);
            } catch (Exception e) {
                context.player.kick("Failed to spawn player");
                throw new RuntimeException("failed to spawn player", e);
            }
        } else if (vfs != null) {
            try {
                thread.load("/player.luau", Objects.requireNonNull(vfs.get("/player.luau")));
                thread.call(0, 0);
            } catch (Exception e) {
                context.player.kick("Failed to spawn player");
                throw new RuntimeException("failed to spawn player", e);
            }
        } else {
            var playerScript = URI.create(Objects.requireNonNull(baseUrl) + "/player.luau");
            try (var is = playerScript.toURL().openStream()) {
                var source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                var bytecode = LUAU_COMPILER.compile(source);
                thread.load("/player.luau", bytecode);
                thread.call(0, 0);
            } catch (Exception e) {
                context.player.kick("Failed to spawn player");
                throw new RuntimeException("failed to spawn player", e);
            }
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
