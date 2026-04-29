package net.hollowcube.luau.slopgen.model;

import com.palantir.javapoet.TypeName;

/// One side of a property — either the getter or the setter. The Java method name and the
/// declaring type together describe how to invoke it from generated code (`enclosingType.method(state)`
/// for statics, `self.method(state)` for instance accessors where the enclosing type is implied).
public record AccessorSpec(
    String javaMethodName,
    TypeName enclosingType
) {}
