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

        // We use the roblox pattern of having a global "script" which can be used to access the "owner" of the script.
        // For now this is kinda dumb since its just the player, HOWEVER it gets around a very cursed optimization case.
        // Luau will eagerly evaluate all __index-es on globals when the script is loaded, meaning that if the player
        // was a global, all occurrences of `player.Position` would be evaluated immediately instead of when they
        // actually occur. Gross.
        state.newTable();
        final var player = event.player();
        LuaPlayer.push(state, new LuaPlayer(player));
        state.setField(-2, "Parent"); // Set the player as the parent
        state.setReadOnly(-1, true); // Make it read-only
        state.setGlobal("script");

        // Create a persistent reference to the thread
        var scriptState = new LuaScriptState(state, player, global.ref(-1));
        state.setThreadData(scriptState);
        player.setTag(LUA_THREAD_REF_TAG, scriptState);
        event.getMapWorld().eventNode().addChild((EventNode) scriptState.eventNode());
        try {
            var bytecode = COMPILER.compile("""
                    local player = script.Parent
                    
                    local function utf8HeadTail(str)
                        local startByte, endByte = utf8.offset(str, 1), utf8.offset(str, 2)
                        if not startByte then return "", "" end
                        if not endByte then return str, "" end
                        return str:sub(startByte, endByte - 1), str:sub(endByte)
                    end
                    
                    local function rotateFirstUtf8CharToEnd(str)
                        local firstChar, rest = utf8HeadTail(str)
                        return rest .. firstChar
                    end
                    
                    local AXES = {
                        -- Index * 90 is the yaw for text displays.
                        vec(-1, 0, 0), -- -X
                        vec(0, 0, -1), -- -Z
                        vec(1, 0, 0), -- +X
                        vec(0, 0, 1), -- +Z
                    }
                    
                    local BIG_TEXT_SCALE = vec(16, 16, 16)
                    local function spawnBigText(pos, color1, color2, text)
                        local entities = {}
                    
                        for index, axis in AXES do
                            for j = 0, 2 do
                                local layerColor = color1:Lerp(color2, j / 2)
                                entities[#entities + 1] = player:SpawnEntity("minecraft:text_display", {
                                    Position = pos + vec(0.5, -0.2, 0.5) + (vec(-0.2, 0, -0.2) * vec(axis.Z, 0, -axis.X)) + (axis * 1.8) + (axis * -0.3 * j),
                                    Yaw = index * 90,
                                    Text = {
                                        Text = text,
                                        Color = layerColor,
                                    },
                                    Scale = BIG_TEXT_SCALE,
                                    Background = 0,
                                })
                            end
                        end
                    
                        return entities
                    end
                    
                    -- TODO NEED TO SET 60 TEXT OPACITY ON INSIDE FACES
                    -- ALSO IT SHOULD ALWAYS BE THE MAX COLOR FOR THAT STACK HEIGHT (60 opa)
                    local LEVEL_TEXT_SCALE = vec(1.25, 1.25, 1.25)
                    local LEVEL_TEXT_STACK_COUNT = 12
                    local function spawnLevelText(pos, cpIndex, color1, color2)
                        -- One list of entities per Y level (bottom to top)
                        local entityStacks = {}
                    
                        -- Repeat for all 4 axes
                        for index, axis in AXES do
                            local text = `*LEVEL_{cpIndex}*LEVEL_{cpIndex}`
                    
                            -- Spawn one set for each Y level in the stack
                            for i = 1, LEVEL_TEXT_STACK_COUNT do
                                local entityStack = entityStacks[i] or {}
                                text = rotateFirstUtf8CharToEnd(text)
                                local displayText = string.gsub(text, "*", "···")
                                local displayColor = color1:Lerp(color2, i / LEVEL_TEXT_STACK_COUNT)
                                local backColor = displayColor * 0.34
                    
                                -- Spawn the backset displays for 3d effect
                                for j = 0, 2 do
                                    local layerColor = displayColor:Lerp(backColor, j / 2)
                                    entityStack[#entityStack + 1] = player:SpawnEntity("minecraft:text_display", {
                                        Position = pos + vec(0.5, 0.4 * (i - 1), 0.5) + (axis * 1.5) + (axis * -0.075 * j),
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
                                    Position = pos + vec(0.5, 0.4 * (i - 1), 0.5) + (axis * 1.5),
                                    Yaw = index * 90 - 180,
                                    Text = {
                                        Text = displayText,
                                        Color = color1,
                                    },
                                    Scale = LEVEL_TEXT_SCALE,
                                    Background = 0,
                                    TeleportDuration = 5,
                                    StartInterpolation = 0,
                                })
                    
                                entityStacks[i] = entityStack
                            end
                        end
                    
                        local entities = {}
                        for _, entityStack in entityStacks do
                            for _, entity in entityStack do
                                entities[#entities + 1] = entity
                            end
                        end
                        return entities, entityStacks
                    end
                    
                    local function levelTextAnimation(entityStacks)
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
                        task.wait(2) -- to 8
                        for _, entity in entityStacks[4] do
                            entity:Teleport(vec(0, -0.360000005, 0), nil, nil, "xyz")
                        end
                        task.wait(1) -- to 7
                        for _, entity in entityStacks[6] do
                            entity:Remove()
                        end
                        for _, entity in entityStacks[3] do
                            entity.TeleportDuration = 4
                            entity:Teleport(vec(0, -0.18, 0), nil, nil, "xyz")
                        end
                        task.wait(2) -- to 5
                        for _, entity in entityStacks[5] do
                            entity:Remove()
                        end
                        for _, entity in entityStacks[2] do
                            entity.TeleportDuration = 4
                            entity:Teleport(vec(0, -0.104, 0), nil, nil, "xyz")
                        end
                        task.wait(2) -- to 3
                        for _, entity in entityStacks[4] do
                            entity:Remove()
                        end
                        for _, entity in entityStacks[3] do
                            entity:Teleport(vec(0, 0.054, 0), nil, nil, "xyz")
                        end
                        task.wait(1) -- to 2
                        for _, entity in entityStacks[2] do
                            entity.TeleportDuration = 3
                            entity:Teleport(vec(0, 0.022, 0), nil, nil, "xyz")
                        end
                    end
                    
                    local CHECKPOINT_COLORS = {
                        --           MIN COLOR             MAX COLOR             X BACK COLOR
                        { Color.new("#ED377D"), Color.new("#FB74D7"), Color.new("#520F45") },
                        { Color.new("#D431B7"), Color.new("#F573E3"), Color.new("#4C0D52") },
                        { Color.new("#8723B8"), Color.new("#E570E4"), Color.new("#3A0A53") },
                        { Color.new("#2881C8"), Color.new("#D084E7"), Color.new("#242056") },
                        { Color.new("#89BC23"), Color.new("#DA96AF"), Color.new("#3A2E2F") },
                        { Color.new("#E6C720"), Color.new("#F799AF"), Color.new("#50302F") },
                        { Color.new("#FF7C11"), Color.new("#FF82AA"), Color.new("#561F2") },
                        { Color.new("#E41235"), Color.new("#F661B5"), Color.new("#4F0634") },
                        { Color.new("#FBD2F0"), Color.new("#FFFFFF"), Color.new("#553460") }, -- todo what is the max finish?
                    }
                    local CHECKPOINT_POSITIONS = {
                        vec(8, 0, 24),
                        vec(25, 1, 57),
                        vec(12, -1, 77),
                        vec(-28, -1, 68),
                        vec(-12, 0, 98),
                        vec(17, -1, 112),
                        vec(48, 2, 60),
                        vec(90, -1, 108),
                        vec(43, 3, 253),
                    }
                    
                    local checkpointStates = {}
                    for i, _ in CHECKPOINT_POSITIONS do
                        checkpointStates[i] = {
                            state = "init", -- "pending", "next", "active", or "completed"
                            entities = {},
                            entityStacks = {}, -- For level text only
                        }
                    end
                    
                    local function updateCheckpointState(cpIndex, newState)
                        local cpState = checkpointStates[cpIndex]
                        if cpState.state == newState then return end
                    
                        cpState.state = newState
                        if newState ~= "active" then
                            for _, entity in cpState.entities do
                                entity:Remove()
                            end
                            cpState.entities = {}
                            cpState.entityStacks = {}
                        end
                    
                        local pos = CHECKPOINT_POSITIONS[cpIndex]
                        local colors = CHECKPOINT_COLORS[cpIndex]
                        if newState == "pending" then
                            cpState.entities = spawnBigText(pos, colors[1], colors[3], "x")
                        elseif newState == "next" then
                            cpState.entities, cpState.entityStacks = spawnLevelText(pos, cpIndex, colors[1], colors[2])
                        elseif newState == "active" then
                            task.spawn(function()
                                levelTextAnimation(cpState.entityStacks)
                            end)
                    
                            player:SetBlock(pos - vec(0, 2, 0), Block.RedstoneLamp{lit = "true"})
                        elseif newState == "completed" then
                            cpState.entities = spawnBigText(pos, colors[1], colors[3], "☺")
                            player:SetBlock(pos - vec(0, 2, 0), Block.RedstoneLamp{lit = "false"})
                        end
                    end
                    
                    local function updateCheckpointStates()
                        local progressIndex = player.ProgressIndex
                        for i, _ in checkpointStates do
                            local newState = ""
                            if i < progressIndex then
                                newState = "completed"
                            elseif i < progressIndex + 1 then
                                newState = "active"
                            elseif i == progressIndex + 1 then
                                newState = "next"
                            elseif i > progressIndex + 1 then
                                newState = "pending"
                            end
                            player:SendMessage(`Checkpoint {i} state: {newState}`)
                            updateCheckpointState(i, newState)
                        end
                    end
                    
                    updateCheckpointStates()
                    
                    local function onCheckpointChanged()
                        player:SpawnParticle(Particle.EndRod, player.Position + vec(0, 1, 0), vec(0, 0, 0), 0.3, 30)
                    
                        player:PlaySound("minecraft:ui.loom.take_result", player.Position, 1.5, 0.6)
                        player:PlaySound("minecraft:entity.cat.ambient", player.Position, 1.5, 0.9)
                        player:PlaySound("minecraft:entity.cat.purr", player.Position, 2, 1)
                    
                        updateCheckpointStates()
                    end
                    
                    player.CheckpointChanged:Listen(onCheckpointChanged)
                    
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
