package net.hollowcube.luau.slopgen.emit;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;

/// Strategy for emitting an atom reference inside a generated `switch` case label. Two
/// implementations:
///
/// - [#literal]: inlines the numeric value. Couples a generated glue class to the per-build atom
///   assignment, making the processor inherently aggregating.
/// - [#symbolic]: emits `LuaStringAtoms.A_<name>` — a compile-time constant maintained by the
///   aggregating atom-table processor. Decouples per-library codegen from atom assignment, which
///   is what enables the per-file processor to be isolating.
@FunctionalInterface
public interface AtomResolver {

    CodeBlock label(String luaName, short value);

    static AtomResolver literal() {
        return (luaName, value) -> CodeBlock.of("$L", value);
    }

    static AtomResolver symbolic(ClassName atomsClass) {
        return (luaName, value) -> CodeBlock.of("$T.$L", atomsClass, AtomNames.javaIdentifier(luaName));
    }
}
