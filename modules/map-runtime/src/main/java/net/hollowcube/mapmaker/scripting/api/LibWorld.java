package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.mapmaker.map.MapWorld;

@LuaLibrary(name = "@mapmaker/world")
public final class LibWorld {

    @LuaExport
    public record World(MapWorld world) {

    }

    public static void pushWorld(LuaState state, MapWorld world) {
        LibWorld$luau.pushWorld(state, new World(world));
    }

}
