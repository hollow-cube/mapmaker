package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.script.PlayerScriptContainer;
import org.jetbrains.annotations.NotNull;

public class LuaSystem {

    public static void initGlobalLib(@NotNull LuaState global) {
        global.newTable();

        global.pushCFunction(state -> {
            if (state.getThreadData() instanceof PlayerScriptContainer psc)
                return psc.schedule(state);
            state.error("unknown issue");
            return 0;
        }, "RunLater");
        global.setField(-2, "RunLater");

        global.setGlobal("system");
    }

}
