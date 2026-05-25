package net.hollowcube.mapmaker.bundle;

import net.hollowcube.luau.compiler.LuauCompiler;

public final class BundleCompiler {

    private BundleCompiler() {
    }

    public static final LuauCompiler PROD = LuauCompiler.builder()
        .vectorType("vector")
        .vectorCtor("vec")
        .build();
}
