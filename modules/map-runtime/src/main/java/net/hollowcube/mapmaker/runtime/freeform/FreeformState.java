package net.hollowcube.mapmaker.runtime.freeform;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.PlayerState;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.freeform.bundle.ScriptBundle;
import net.hollowcube.mapmaker.runtime.freeform.lua.player.LuaPlayer;
import net.hollowcube.mapmaker.runtime.freeform.lua.world.LuaWorld;
import net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers;
import net.hollowcube.mapmaker.runtime.freeform.script.LuaScriptState;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public sealed interface FreeformState extends PlayerState<FreeformState, FreeformMapWorld> {
    Logger log = LoggerFactory.getLogger(FreeformState.class);

    record Playing(SaveState saveState, List<LuaScriptState> activeScripts) implements FreeformState {

        @Override
        public void configurePlayer(FreeformMapWorld world, Player player, @Nullable FreeformState lastState) {
            FreeformState.super.configurePlayer(world, player, lastState);
            for (var entrypoint : world.scriptBundle().entrypoints()) {
                if (entrypoint.type() != ScriptBundle.Entrypoint.Type.PLAYER)
                    continue;

                try {
                    var script = world.scriptBundle().loadScript(entrypoint.script());
                    log.info("Loading world script {}", script.filename());

                    var thread = LuaScriptState.create(world);
                    activeScripts.add(thread);

                    JsonObject saveData = new JsonObject();
                    var rawSaveData = saveState.state(ScriptState.class).saveData();
                    if (rawSaveData != null) {
                        var parsed = rawSaveData.convertTo(Transcoder.JSON).orElse(null);
                        if (parsed != null) saveData = parsed.getAsJsonObject();
                    }

                    // We use the roblox pattern of having a global "script" which can be used to access the "owner" of the script.
                    // For now this is kinda dumb since its just the world/player, HOWEVER it gets around a very cursed optimization.
                    // Luau will eagerly evaluate all __index-es on globals when the script is loaded, meaning that if the player
                    // was a global, all occurrences of `player.Position` would be evaluated immediately instead of when they
                    // actually occur. Gross.
                    // todo whole thing is duped
                    thread.state().newTable();
                    LuaPlayer.push(thread.state(), new LuaPlayer(thread.state(), player, saveData));
                    thread.state().setField(-2, "Parent"); // Set the player as the parent
                    LuaWorld.push(thread.state(), new LuaWorld(world));
                    thread.state().setField(-2, "World"); // todo want to expose world on game object instead of script object
                    thread.state().setReadOnly(-1, true); // Make it read-only
                    thread.state().setGlobal("script");

                    thread.state().load(script.filename(), FreeformMapWorld.LUAU_COMPILER.compile(script.content()));
                    thread.state().pcall(0, 0);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load world script " + entrypoint.script(), e);
                }
            }
        }

        @Override
        public void resetPlayer(FreeformMapWorld world, Player player, @Nullable FreeformState nextState) {
            FreeformState.super.resetPlayer(world, player, nextState);

            if (!activeScripts.isEmpty()) {
                // todo: dont access the savedata table in such a cursed way :skull:
                var state = activeScripts.getFirst().state();
                state.getGlobal("script");
                state.getField(-1, "Parent");
                state.getField(-1, "SaveData");
                var json = LuaHelpers.readJsonElement(state, -1);
                saveState.state(ScriptState.class).saveData(
                        Codec.RawValue.of(Transcoder.JSON, json)
                );
                state.pop(4);
            }

            FutureUtil.submitVirtual(() -> writeSaveState(world, player, saveState));

            activeScripts.forEach(LuaScriptState::close);
            activeScripts.clear();
        }

        private static void writeSaveState(FreeformMapWorld world, Player player, SaveState saveState) {
            var update = saveState.createUpsertRequest();
            update.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));

            // Write the save state to the database
            try {
                var playerData = PlayerData.fromPlayer(player);
                world.server().mapService().updateSaveState(
                        world.map().id(), playerData.id(), saveState.id(), update);
            } catch (Exception e) {
                var wrappedException = new RuntimeException("failed to save player save state", e);
                ExceptionReporter.reportException(wrappedException, player);
            }
        }

    }

}
