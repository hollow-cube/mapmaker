package net.hollowcube.luau.slopgen.model;

import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.slopgen.docs.MemberDocs;

/// One side of a property — either the getter or the setter. The Java method name and the
/// declaring type together describe how to invoke it from generated code (`enclosingType.method(state)`
/// for statics, `self.method(state)` for instance accessors where the enclosing type is implied).
///
/// Each accessor carries its own [MemberDocs] so getter/setter doc comments stay independent;
/// the docs module pulls type info from the getter's `@luaReturn` and the setter's `@luaParam`.
public record AccessorSpec(
    String javaMethodName,
    TypeName enclosingType,
    MemberDocs docs
) {}
