package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuaGlobals {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuaGlobals.class);

    private static final LuaFunc PRINT = LuaFunc.wrap(LuaGlobals::print, "print");

    public static void register(LuaState state) {
        state.pushFunction(PRINT);
        state.setGlobal("print");
    }

    private static int print(LuaState state) {
        var builder = new StringBuilder();
        int top = state.top();
        for (int i = 1; i <= top; i++) {
            var arg = state.toStringRepr(i);
            if (i > 1) builder.append(" ");
            builder.append(arg);
        }

        LOGGER.info("[SCRIPT] {}", builder);

        if (ServerRuntime.getRuntime().isDevelopment()) {
            var world = switch (ScriptContext.get(state)) {
                case ScriptContext.World w -> w.world().instance();
                case ScriptContext.Player p -> p.player().getInstance();
            };
            world.sendMessage(Component.text("[SCRIPT] " + builder));
        }

        return 0;
    }
}
