package net.hollowcube.mapmaker.map.scripting;

import com.google.auto.service.AutoService;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.scripting.api.LuaColor;
import net.hollowcube.mapmaker.map.scripting.api.LuaEventSource;
import net.hollowcube.mapmaker.map.scripting.api.LuaLibTask;
import net.hollowcube.mapmaker.map.scripting.api.LuaText;
import net.hollowcube.mapmaker.map.scripting.api.entity.LuaEntity;
import net.hollowcube.mapmaker.map.scripting.api.entity.LuaPlayer;
import net.hollowcube.mapmaker.map.scripting.api.math.LuaVectorTypeImpl;
import net.hollowcube.mapmaker.map.scripting.api.world.LuaBlock;
import net.hollowcube.mapmaker.map.scripting.api.world.LuaParticle;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/*

Open Questions about scripting:
* How should we type things that take/return text components?
* How should we type colors
  * Roblox has a Color3 which probably makes sense to do something similar: https://create.roblox.com/docs/reference/engine/datatypes/Color3

 */
@AutoService(FeatureProvider.class)
public class ScriptingFeatureProvider implements FeatureProvider {
    private static final String QB_DG = "5ede93ae-493a-4318-963e-7fa6ac7fc30b";

    private static final Tag<LuaState> LUA_STATE_TAG = Tag.Transient("mapmaker:play/lua_state");

    public static final Tag<LuaScriptState> LUA_THREAD_REF_TAG = Tag.Transient("mapmaker:play/lua_thread_ref");

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
        if (!(event.isFirstInit())) return;
        var global = Objects.requireNonNull(event.getMapWorld().getTag(LUA_STATE_TAG));

        var state = global.newThread();
        state.sandboxThread();

        final var player = event.player();
        LuaPlayer.push(state, new LuaPlayer(player));
        state.setGlobal("player");

        // Create a persistent reference to the thread
        var scriptState = new LuaScriptState(state, player, global.ref(-1));
        state.setThreadData(scriptState);
        player.setTag(LUA_THREAD_REF_TAG, scriptState);
        event.getMapWorld().eventNode().addChild((EventNode) scriptState.eventNode());

        try {
            var bytecode = COMPILER.compile("""
                    
                    local function onCheckpointChanged()
                        player:SpawnParticle(Particle.EndRod, player.Position + vec(0, 1, 0), vec(0, 0, 0), 0.3, 30)
                    
                        player:PlaySound("minecraft:ui.loom.take_result", player.Position, 1.5, 0.6)
                        player:PlaySound("minecraft:entity.cat.ambient", player.Position, 1.5, 0.9)
                        player:PlaySound("minecraft:entity.cat.purr", player.Position, 2, 1)
                    
                        player:SetBlock(vec(8, -2, 24), Block.RedstoneLamp{lit = "true"})
                    
                        player:SendMessage("<pride>Checkpoint changed!")
                    end
                    
                    player.CheckpointChanged:Listen(onCheckpointChanged)
                    
                    local function utf8HeadTail(str)
                        local startByte, endByte = utf8.offset(str, 1), utf8.offset(str, 2)
                        if not startByte then return "", "" end
                        if not endByte then return str, "" end
                        return str:sub(startByte, endByte - 1), str:sub(endByte)
                    end
                    
                    function rotateFirstUtf8CharToEnd(str)
                        local firstChar, rest = utf8HeadTail(str)
                        return rest .. firstChar
                    end
                    
                    local LEVEL_TEXT_SCALE = vec(1.25, 1.25, 1.25)
                    local AXES = {
                        -- Index * 90 is the yaw for text displays.
                        vec(-1, 0, 0), -- -X
                        vec(0, 0, -1), -- -Z
                        vec(1, 0, 0), -- +X
                        vec(0, 0, 1), -- +Z
                    }
                    local MIN_COLOR = Color.new("#ED377D")
                    local MAX_COLOR = Color.new("#FB74D7")
                    task.spawn(function()
                        local cp1 = vec(8.5, 0, 24.5)
                        local stackCount = 12
                    
                        -- One list of entities per Y level (bottom to top)
                        local entityStacks = {}
                    
                        -- Repeat for all 4 axes
                        for index, axis in AXES do
                            local text = "*LEVEL_1*LEVEL_1"
                    
                            -- Spawn one set for each Y level in the stack
                            for i = 1, stackCount do
                                local entityStack = entityStacks[i] or {}
                                text = rotateFirstUtf8CharToEnd(text)
                                local displayText = string.gsub(text, "*", "···")
                                local displayColor = MIN_COLOR:Lerp(MAX_COLOR, i / stackCount)
                                local backColor = displayColor * 0.34
                    
                                -- Spawn the backset displays for 3d effect
                                for j = 0, 2 do
                                    local layerColor = displayColor:Lerp(backColor, j / 2)
                                    entityStack[#entityStack + 1] = player:SpawnEntity("minecraft:text_display", {
                                        Position = cp1 + vec(0, 0.4 * (i - 1), 0) + (axis * 1.5) + (axis * -0.075 * j),
                                        Yaw = index * 90,
                                        Text = {
                                            Text = displayText,
                                            Color = layerColor,
                                        },
                                        Scale = LEVEL_TEXT_SCALE,
                                        Background = 0,
                                        TeleportDuration = 5,
                                        StartInterpolation = 0,
                                    })
                                end
                    
                                -- One more entity for the inner face
                                entityStack[#entityStack + 1] = player:SpawnEntity("minecraft:text_display", {
                                    Position = cp1 + vec(0, 0.4 * (i - 1), 0) + (axis * 1.5),
                                    Yaw = index * 90 - 180,
                                    Text = {
                                        Text = displayText,
                                        Color = MIN_COLOR,
                                    },
                                    Scale = LEVEL_TEXT_SCALE,
                                    Background = 0,
                                    TeleportDuration = 5,
                                    StartInterpolation = 0,
                                })
                    
                                entityStacks[i] = entityStack
                            end
                        end
                    
                        -- Do the drop effect
                        task.wait(32) -- to 43
                    
                        for _, entity in entityStacks[12] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(5) -- to 38
                        for _, entity in entityStacks[12] do
                            entity:Remove()
                        end
                    
                        task.wait(4) -- to 34
                        for _, entity in entityStacks[11] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(5) -- to 29
                        for _, entity in entityStacks[11] do
                            entity:Remove()
                        end
                    
                        task.wait(2) -- to 27
                        for _, entity in entityStacks[10] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(5) -- to 22
                        for _, entity in entityStacks[10] do
                            entity:Remove()
                        end
                    
                        for _, entity in entityStacks[9] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(4) -- to 18
                        for _, entity in entityStacks[8] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(1) -- to 17
                        for _, entity in entityStacks[9] do
                            entity:Remove()
                        end
                        task.wait(2) -- to 15
                        for _, entity in entityStacks[7] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(2) -- to 13
                        for _, entity in entityStacks[8] do
                            entity:Remove()
                        end
                        task.wait(1) -- to 12
                        for _, entity in entityStacks[6] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(2) -- to 10
                        for _, entity in entityStacks[7] do
                            entity:Remove()
                        end
                        for _, entity in entityStacks[5] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(3) -- to 7 WE MISSED A STEP
                        for _, entity in entityStacks[6] do
                            entity:Remove()
                        end
                        task.wait(2) -- to 5 WE MISSED A STEP
                        for _, entity in entityStacks[5] do
                            entity:Remove()
                        end
                    end)
                    """);

            state.load("test.luau", bytecode);
            state.pcall(0, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /*

         */
    }

    private void deinitPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var global = Objects.requireNonNull(event.getMapWorld().getTag(LUA_STATE_TAG));

        var player = event.getPlayer();
        var scriptState = Objects.requireNonNull(player.getAndSetTag(LUA_THREAD_REF_TAG, null));
        event.getMapWorld().eventNode().removeChild((EventNode) scriptState.eventNode());

        global.unref(scriptState.mainRef());
    }

    private @NotNull LuaState createGlobalState() {
        var global = LuaState.newState();
        global.openLibs(); // todo probably dont give all for now
        LuaLibTask.init(global);

        LuaVectorTypeImpl.init(global);
        LuaColor.init(global);
        LuaText.init(global);

        LuaEventSource.init(global);
        LuaBlock.init(global);
        LuaParticle.init(global);
        LuaEntity.init(global);
        LuaPlayer.init(global);

        global.sandbox();
        return global;
    }
}
