package net.hollowcube.luau.slopgen.model;

import com.palantir.javapoet.TypeName;

/// A non-meta `@LuaMethod` on either a library (static) or an export (instance). For statics
/// the call is emitted as `enclosingType.javaMethodName(state)`; for instance methods on an
/// `@LuaExport`, the call is emitted as `self.javaMethodName(state)` and `enclosingType` is
/// informational (matches the `ExportSpec.javaType` containing this method).
public record MethodSpec(
    String luaName,
    String javaMethodName,
    boolean isVoid,
    TypeName enclosingType
) {}
