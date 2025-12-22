package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaLibrary.Scope;
import net.hollowcube.luau.gen.LuaProperty;

import java.lang.management.ManagementFactory;

@LuaLibrary(name = "runtime", scope = Scope.GLOBAL)
public final class LuaRuntime {

    //region Properties

    @LuaProperty
    public static int getVersion(LuaState state) {
        state.pushString(ServerRuntime.getRuntime().version());
        return 1;
    }

    @LuaProperty
    public static int getBuild(LuaState state) {
        state.pushString(ServerRuntime.getRuntime().shortCommit());
        return 1;
    }

    @LuaProperty
    public static int getSize(LuaState state) {
        state.pushString(ServerRuntime.getRuntime().size());
        return 1;
    }

    @LuaProperty
    public static int getAge(LuaState state) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        state.pushNumber(uptime / 1000.0);
        return 1;
    }

    // TODO: cpu and memory objects

    @LuaProperty
    public static int getHot(LuaState state) {
        // todo should be dependent on whether hot reload is supported in the current context
        LuaRuntime$luau.pushHot(state, new Hot());
        return 1;
    }

    //endregion

    // TODO: this could all be static, should support singletons somehow somewhere in slopgen
    @LuaExport
    public record Hot() {

    }

}
