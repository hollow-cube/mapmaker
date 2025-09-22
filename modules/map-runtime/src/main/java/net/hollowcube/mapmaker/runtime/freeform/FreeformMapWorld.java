package net.hollowcube.mapmaker.runtime.freeform;

import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.freeform.bundle.ScriptBundle;
import net.hollowcube.mapmaker.runtime.freeform.lua.LuaEventSource;
import net.hollowcube.mapmaker.runtime.freeform.lua.LuaGlobals;
import net.hollowcube.mapmaker.runtime.freeform.lua.LuaTask;
import net.hollowcube.mapmaker.runtime.freeform.lua.base.LuaTextImpl$luau;
import net.hollowcube.mapmaker.runtime.freeform.lua.math.LuaVectorTypeImpl;
import net.hollowcube.mapmaker.runtime.freeform.lua.player.LuaPlayer$luau;
import net.hollowcube.mapmaker.runtime.freeform.lua.player.LuaSidebar$luau;
import net.hollowcube.mapmaker.runtime.freeform.lua.world.LuaBlockImpl$luau;
import net.hollowcube.mapmaker.runtime.freeform.lua.world.LuaWorld;
import net.hollowcube.mapmaker.runtime.freeform.lua.world.LuaWorld$luau;
import net.hollowcube.mapmaker.runtime.freeform.script.LuaScriptState;
import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FreeformMapWorld extends AbstractMapWorld<FreeformState, FreeformMapWorld> {

    public static final LuauCompiler LUAU_COMPILER = LuauCompiler.builder()
            .userdataTypes() // todo
            .vectorType("vector")
            .vectorCtor("vec")
            .build();
    private static final Logger log = LoggerFactory.getLogger(FreeformMapWorld.class);

    private final ScriptBundle scriptBundle;

    private final LuaState globalState;
    private final List<LuaScriptState> worldThreads = new ArrayList<>();

    public FreeformMapWorld(MapServer server, MapData map, ScriptBundle.Loader scriptLoader) {
        super(server, map, makeMapInstance(map, 'f'), FreeformState.class);

        var scriptBundle = scriptLoader.load(map.id());
        this.scriptBundle = Objects.requireNonNull(scriptBundle, "Failed to load script bundle for map " + map.id());

        this.globalState = createGlobalState();
    }

    public ScriptBundle scriptBundle() {
        return this.scriptBundle;
    }

    public LuaState globalState() {
        return this.globalState;
    }

    //region World Lifecycle

    @Override
    public void loadWorld() {
        super.loadWorld();

        for (var entrypoint : scriptBundle.entrypoints()) {
            if (entrypoint.type() != ScriptBundle.Entrypoint.Type.WORLD)
                continue;

            try {
                var script = scriptBundle.loadScript(entrypoint.script());
                log.info("Loading world script {}", script.filename());

                var thread = LuaScriptState.create(this);
                this.worldThreads.add(thread);

                // We use the roblox pattern of having a global "script" which can be used to access the "owner" of the script.
                // For now this is kinda dumb since its just the world/player, HOWEVER it gets around a very cursed optimization.
                // Luau will eagerly evaluate all __index-es on globals when the script is loaded, meaning that if the player
                // was a global, all occurrences of `player.Position` would be evaluated immediately instead of when they
                // actually occur. Gross.
                thread.state().newTable();
                LuaWorld.push(thread.state(), new LuaWorld(this));
                thread.state().setField(-2, "Parent"); // Set the world as the parent
                thread.state().setReadOnly(-1, true); // Make it read-only
                thread.state().setGlobal("script");

                thread.state().load(script.filename(), LUAU_COMPILER.compile(script.content()));
                thread.state().pcall(0, 0);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load world script " + entrypoint.script(), e);
            }
        }
    }

    @Override
    public void close() {
        super.close();

        this.worldThreads.forEach(LuaScriptState::close);
        this.globalState.close();
    }

    //endregion

    //region Player Lifecycle

    @Override
    protected FreeformState configurePlayer(Player player) {
        final var playerData = PlayerData.fromPlayer(player);
        SaveState saveState;
        try {
            saveState = server().mapService().getLatestSaveState(map().id(),
                    playerData.id(), SaveStateType.PLAYING, ScriptState.SERIALIZER);
        } catch (MapService.NotFoundError ignored) {
            // No save state yet, create one locally.
            // We do an upsert to save, so it will be created in the map service at that point.
            saveState = new SaveState(UUID.randomUUID().toString(),
                    map().id(), playerData.id(), SaveStateType.PLAYING,
                    ScriptState.SERIALIZER, new ScriptState(null));
            saveState.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));
        }

        player.setRespawnPoint(map().settings().getSpawnPoint());

        return new FreeformState.Playing(saveState, new ArrayList<>());
    }


    @Override
    protected @Nullable List<BossBar> createBossBars() {
        return BossBars.createPlayingBossBar(server().playerService(), map());
    }

    //endregion

    private static LuaState createGlobalState() {
        var global = LuaState.newState();
        global.openLibs(); // todo probably dont give all for now

        // 'Standard' Libraries
        LuaGlobals.init(global);
        LuaTask.init(global);

        // Global APIs
        LuaVectorTypeImpl.init(global);
//        LuaColor.init(global);
        LuaTextImpl$luau.init$luau(global);

        LuaEventSource.init(global);
        LuaBlockImpl$luau.init$luau(global);
        LuaWorld$luau.init$luau(global);
//        LuaParticle.init(global);
//        LuaEntity.init(global);

        // Player & friends
        LuaPlayer$luau.init$luau(global);
        LuaSidebar$luau.init$luau(global);

        // TODO for gen
        //  - use tagged user data (and a more generic way to add to state)
        //  - use service files to discover impls and load them.

        global.sandbox();
        return global;
    }
}
