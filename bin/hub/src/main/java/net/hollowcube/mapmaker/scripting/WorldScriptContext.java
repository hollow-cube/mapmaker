package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.scripting.api.LibBase$luau;
import net.hollowcube.mapmaker.scripting.api.LibEnv$luau;
import net.hollowcube.mapmaker.scripting.api.LibPlayer$luau;
import net.hollowcube.mapmaker.scripting.api.LibTask$luau;
import net.hollowcube.mapmaker.scripting.require.ResourceRequireResolver;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorldScriptContext {
    private static final Logger LOGGER = Logger.getLogger(WorldScriptContext.class.getName());

    private static final LuaFunc PRINT = LuaFunc.wrap(WorldScriptContext::print, "print");

    private static final LuauCompiler LUAU_COMPILER = LuauCompiler.builder()
        .userdataTypes() // todo
        .vectorType("vector")
        .vectorCtor("vec")
        .build();

    private final LuaState state;

    public WorldScriptContext(URI baseUrl) {
        this.state = LuaState.newState();
        state.openLibs();

        state.openRequire(new ResourceRequireResolver(LUAU_COMPILER, baseUrl));
        registerGeneratedStringAtoms(state);

        state.pushFunction(PRINT);
        state.setGlobal("print");

        LibBase$luau.register(state);
        LibTask$luau.register(state);
        LibEnv$luau.register(state);
        LibPlayer$luau.register(state);

        state.sandbox();
    }

    public LuaState createThread() {
        state.newThread();
        state.sandboxThread();

        var _ = state.ref(-1); // TODO: memory leak
        state.pop(1); // remove the thread

        return state;
    }

    private static void registerGeneratedStringAtoms(LuaState state) {
        // todo cache this or something
        try {
            Class.forName("net.hollowcube.luau.gen.runtime.GeneratedStringAtoms")
                .getDeclaredMethod("register", LuaState.class)
                .invoke(null, state);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int print(LuaState state) {
        var builder = new StringBuilder();
        int top = state.top();
        for (int i = 1; i <= top; i++) {
            var arg = state.toStringRepr(i);
            if (i > 1) builder.append(" ");
            builder.append(arg);
        }

        // TODO: should include debug info in here later
//        var script = LuaScriptState.from(state);
//        var world = script.world();

        LOGGER.log(Level.INFO, "[SCRIPT] {0}", builder);
//        world.instance().sendMessage(Component.text("[SCRIPT] " + builder));

        return 0;
    }


}
