package net.hollowcube.luau.slopgen.model;

import net.hollowcube.luau.gen.Meta;
import net.hollowcube.luau.slopgen.docs.MemberDocs;

/// A metamethod implementation on an `@LuaExport`. The implementing Java method is invoked
/// as `self.javaMethodName(state)` after the receiver argument is stripped from the stack,
/// regardless of `meta`'s identity.
public record MetaSpec(
    Meta meta,
    String javaMethodName,
    boolean isVoid,
    MemberDocs docs
) {}
