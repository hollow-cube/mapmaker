package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.scripting.require.ResourceRequireResolver;

import java.net.URI;
import java.util.Objects;

public class TestingSlop {
    static void main() throws Exception {
        var playerScript = Objects.requireNonNull(TestingSlop.class.getResource("/scripts/player.luau"));
        var baseUrl = URI.create(playerScript.toString().substring(0, playerScript.toString().lastIndexOf('/')));

        try (var state = LuaState.newState()) {
            state.openLibs();
            state.openRequire(new ResourceRequireResolver(LuauCompiler.DEFAULT, baseUrl));
            state.sandbox();

            {
                state.newThread();
                state.sandboxThread();

                var bytecode = LuauCompiler.DEFAULT.compile("""
                    local a = require('./other');
                    local b = require('./other');
                    
                    print(b.sayHello());
                    print(a.explode());
                    """);
                state.load("/player.luau", bytecode);
                state.call(0, 0);
            }

            state.pop(1);
        }
    }
}
