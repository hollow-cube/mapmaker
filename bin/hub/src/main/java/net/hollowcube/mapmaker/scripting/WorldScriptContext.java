package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.scripting.require.ResourceRequireResolver;

import java.net.URI;

public class WorldScriptContext {
    private static final LuauCompiler LUAU_COMPILER = LuauCompiler.builder()
        .userdataTypes() // todo
        .vectorType("vector")
        .vectorCtor("vec")
        .build();

    private final LuaState state;

    public WorldScriptContext(URI baseUrl) {
        this.state = LuaState.newState();
        state.openLibs();

        state.openRequire(new ResourceRequireResolver(LuauCompiler.DEFAULT, baseUrl));

        state.sandbox();
    }

    public LuaState createThread() {
        state.newThread();
        state.sandboxThread();

        var _ = state.ref(-1); // TODO: memory leak
        state.pop(1); // remove the thread

        return state;
    }


}
