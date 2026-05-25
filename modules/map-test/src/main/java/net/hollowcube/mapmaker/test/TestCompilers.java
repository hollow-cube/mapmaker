package net.hollowcube.mapmaker.test;

import net.hollowcube.luau.compiler.LuauCompiler;

public final class TestCompilers {

    public static final LuauCompiler EDITOR = LuauCompiler.builder()
        .userdataTypes() // todo
        .vectorType("vector")
        .vectorCtor("vec")
        .build();

    private TestCompilers() {
    }
}
