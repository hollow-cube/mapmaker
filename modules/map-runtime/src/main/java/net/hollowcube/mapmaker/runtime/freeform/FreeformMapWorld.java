package net.hollowcube.mapmaker.runtime.freeform;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.runtime.freeform.lua.LuaGlobals;
import net.hollowcube.mapmaker.runtime.freeform.lua.LuaTask;
import net.hollowcube.mapmaker.runtime.freeform.lua.math.LuaVectorTypeImpl;
import net.hollowcube.mapmaker.runtime.freeform.lua.world.LuaBlock;
import net.hollowcube.mapmaker.runtime.freeform.lua.world.LuaWorld;
import net.hollowcube.mapmaker.runtime.freeform.script.LuaScriptState;
import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FreeformMapWorld extends AbstractMapWorld<FreeformState, FreeformMapWorld> {

    private static final LuauCompiler LUAU_COMPILER = LuauCompiler.builder()
            .userdataTypes() // todo
            .vectorType("vector")
            .vectorCtor("vec")
            .build();

    private final LuaState globalState;
    private LuaScriptState worldThread;

    public FreeformMapWorld(MapServer server, MapData map) {
        super(server, map, makeMapInstance(map, 'f'), FreeformState.class);

        this.globalState = createGlobalState();

        // 1, 39, 1
        // -62, 39, -62

//        eventNode()
//                .addListener(PlayerTickEvent.class, this::handlePlayerTick)
//                .addChild(EventUtil.READ_ONLY_NODE);
    }

    public LuaState globalState() {
        return this.globalState;
    }

    //region World Lifecycle

    @Override
    public void loadWorld() {
        super.loadWorld();

        this.worldThread = LuaScriptState.create(this);
        try {

            // We use the roblox pattern of having a global "script" which can be used to access the "owner" of the script.
            // For now this is kinda dumb since its just the world/player, HOWEVER it gets around a very cursed optimization.
            // Luau will eagerly evaluate all __index-es on globals when the script is loaded, meaning that if the player
            // was a global, all occurrences of `player.Position` would be evaluated immediately instead of when they
            // actually occur. Gross.
            this.worldThread.state().newTable();
            LuaWorld.push(this.worldThread.state(), new LuaWorld(this));
            this.worldThread.state().setField(-2, "Parent"); // Set the world as the parent
            this.worldThread.state().setReadOnly(-1, true); // Make it read-only
            this.worldThread.state().setGlobal("script");

            worldThread.state().load("test.luau", LUAU_COMPILER.compile("""
                    local world = script.Parent
                    
                    function create_bit_board(width, height)
                        local bits_needed = width * height
                        local bytes_needed = math.ceil(bits_needed / 8)
                        return buffer.create(bytes_needed), width, height
                    end
                    
                    function get_cell(board, width, x, y)
                        local bit_index = y * width + x
                        return buffer.readbits(board, bit_index, 1)
                    end
                    
                    function set_cell(board, width, x, y, value)
                        local bit_index = y * width + x
                        buffer.writebits(board, bit_index, 1, value and 1 or 0)
                    end
                    
                    function count_neighbors_bitwise(board, width, height, x, y)
                        local count = 0
                        local base_bit = y * width + x
                    
                        for dy = -1, 1 do
                            for dx = -1, 1 do
                                if dx ~= 0 or dy ~= 0 then
                                    local nx, ny = x + dx, y + dy
                                    if nx >= 0 and nx < width and ny >= 0 and ny < height then
                                        local neighbor_bit = (y + dy) * width + (x + dx)
                                        count = count + buffer.readbits(board, neighbor_bit, 1)
                                    end
                                end
                            end
                        end
                        return count
                    end
                    
                    local worldSpace = create_bit_board(64, 64)
                    local copySpace = create_bit_board(64, 64)
                    
                    -- init world
                    for x = 0, 63 do
                        for z = 0, 63 do
                            local idx = x * 64 + z
                            local active = world:GetBlock(vec(-x, 39, -z)) == Block.Stone
                            set_cell(worldSpace, 64, x, z, active)
                        end
                    end
                    
                    function step()
                        buffer.copy(copySpace, 0, worldSpace, 0, math.ceil(64 * 64 / 8))
                    
                        for x = 0, 63 do
                            for z = 0, 63 do
                                local idx = x * 64 + z
                                local active = get_cell(copySpace, 64, x, z)
                                local neighbors = count_neighbors_bitwise(copySpace, 64, 64, x, z)
                    
                                if active then
                                    -- A live cell with fewer than two live neighbors dies.
                                    -- A live cell with more than three live neighbors dies.
                                    if neighbors < 2 or neighbors > 3 then
                                        set_cell(worldSpace, 64, x, z, false)
                                        world:SetBlock(vec(-x, 39, -z), Block.Air)
                                    end
                                    -- A live cell with two or three live neighbors continues to live on to the next generation.
                                else
                                    -- A dead cell with exactly three live neighbors becomes a live cell in the next generation.
                                    if neighbors == 3 then
                                        set_cell(worldSpace, 64, x, z, true)
                                        world:SetBlock(vec(-x, 39, -z), Block.Stone)
                                    end
                                end
                            end
                        end
                    
                    end
                    
                    task.spawn(function()
                        print('Hello, Task!')
                        task.wait(80)
                        print('should be printed later')
                    
                        step()
                        task.wait(20)
                        step()
                        task.wait(20)
                        step()
                        task.wait(20)
                        step()
                        task.wait(20)
                        step()
                        task.wait(20)
                        step()
                    end)
                    """));
            worldThread.state().pcall(0, 0);
        } catch (LuauCompileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        super.close();

        this.worldThread.close();
        this.globalState.close();
    }

    //endregion

    //region Player Lifecycle

    @Override
    protected FreeformState configurePlayer(Player player) {

        player.setRespawnPoint(map().settings().getSpawnPoint());

        return new FreeformState.Playing();
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
//        LuaText.init(global);

//        LuaEventSource.init(global);
        LuaBlock.init(global);
        LuaWorld.init(global);
//        LuaParticle.init(global);
//        LuaEntity.init(global);
//        LuaPlayer.init(global);

        global.sandbox();
        return global;
    }
}
