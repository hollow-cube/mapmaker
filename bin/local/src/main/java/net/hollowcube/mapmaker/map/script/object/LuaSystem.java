package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.mapmaker.map.script.PlayerScriptContainer;
import org.jetbrains.annotations.NotNull;

@LuaObject
public final class LuaSystem {
    public static final LuaSystem INSTANCE = new LuaSystem();

    private LuaSystem() {
    }

    @LuaMethod
    public int runLater(@NotNull LuaState state) {
        if (state.getThreadData() instanceof PlayerScriptContainer psc)
            return psc.schedule(state);
        state.error("unknown issue");
        return 0;
    }

}
