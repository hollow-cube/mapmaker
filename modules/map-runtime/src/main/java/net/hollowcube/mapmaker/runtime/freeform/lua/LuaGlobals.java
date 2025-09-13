package net.hollowcube.mapmaker.runtime.freeform.lua;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.runtime.freeform.script.LuaScriptState;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuaGlobals {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuaGlobals.class);

    public static void init(LuaState state) {
        state.pushCFunction(LuaGlobals::print, "print");
        state.setGlobal("print");
    }

    private static int print(LuaState state) {
        var builder = new StringBuilder();
        int top = state.getTop();
        for (int i = 1; i <= top; i++) {
            var arg = state.toStringRepr(i);
            if (i > 1) builder.append(" ");
            builder.append(arg);
        }

        // TODO: should include debug info in here later
        var script = LuaScriptState.from(state);
        var world = script.world();

        LOGGER.info("[SCRIPT] {}", builder);
        world.instance().sendMessage(Component.text("[SCRIPT] " + builder));

        return 0;
    }
}
