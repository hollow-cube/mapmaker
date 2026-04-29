package net.hollowcube.luau.slopgen.emit;

import com.palantir.javapoet.*;
import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.LuaNames;
import net.hollowcube.luau.slopgen.model.AtomTable;
import net.hollowcube.luau.slopgen.model.ExportSpec;
import net.hollowcube.luau.slopgen.model.LibrarySpec;
import net.hollowcube.luau.slopgen.model.MetaSpec;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.List;

/// Translates a [LibrarySpec] into the generated `<Library>$luau.java` companion class. Field
/// and method ordering matches the original visitor for byte-equivalent output on existing inputs;
/// the emit pass is otherwise free of AST-traversal state.
public final class LibraryEmitter {

    private static final ParameterSpec NOT_NULL_STATE = ParameterSpec.builder(LuaState.class, "state")
        .addAnnotation(NotNull.class)
        .build();

    private final AtomTable atomTable;
    private final AtomResolver atomResolver;

    public LibraryEmitter(AtomTable atomTable, AtomResolver atomResolver) {
        this.atomTable = atomTable;
        this.atomResolver = atomResolver;
    }

    public JavaFile emit(LibrarySpec spec) {
        return emit(spec, new Element[0]);
    }

    /// Emit a library with originating elements attached to the generated TypeSpec. Gradle uses
    /// the originating elements to associate the generated source with its input file — without
    /// them, isolating annotation processing falls back to non-incremental.
    public JavaFile emit(LibrarySpec spec, Element... originatingElements) {
        var glue = TypeSpec.classBuilder(spec.glueType())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        for (var e : originatingElements) glue.addOriginatingElement(e);

        // ---------- Fields ----------
        glue.addField(FieldSpec.builder(String.class, "LIB_NAME")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", spec.moduleName())
            .build());
        glue.addField(metaWrapperField("INDEX", spec.glueType(), "index$meta", LuaNames.INDEX_META_NAME));
        glue.addField(metaWrapperField("NEWINDEX", spec.glueType(), "newindex$meta", LuaNames.NEWINDEX_META_NAME));
        glue.addField(metaWrapperField("NAMECALL", spec.glueType(), "namecall$meta", LuaNames.NAMECALL_META_NAME));

        // Per-export header fields. For final/leaf exports the meta wrapper LuaFunc fields are
        // emitted up-front (these wrap the body method directly). For dispatch roots they are
        // emitted later, after the body methods exist.
        for (var ex : spec.exports()) {
            if (!isRoot(ex)) continue;

            if (!isDispatch(ex)) {
                glue.addField(metaWrapperField(upperPrefix(ex, "INDEX"), spec.glueType(), lower(ex.luaName()) + "$index$meta", LuaNames.INDEX_META_NAME));
                glue.addField(metaWrapperField(upperPrefix(ex, "NEWINDEX"), spec.glueType(), lower(ex.luaName()) + "$newindex$meta", LuaNames.NEWINDEX_META_NAME));
                glue.addField(metaWrapperField(upperPrefix(ex, "NAMECALL"), spec.glueType(), lower(ex.luaName()) + "$namecall$meta", LuaNames.NAMECALL_META_NAME));
            }
            glue.addField(FieldSpec.builder(int.class, upperPrefix(ex, "TAG"))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", ex.userDataTag())
                .build());

            // Meta method LuaFunc fields (these wrap the per-meta body method, and are added in
            // source-declaration order of the meta methods within the export).
            for (var m : ex.metaMethods()) {
                glue.addField(metaWrapperField(
                    upperPrefix(ex, m.javaMethodName().toUpperCase()),
                    spec.glueType(),
                    lower(ex.luaName()) + "$" + m.javaMethodName(),
                    m.meta().methodName()));
            }
        }

        // Static method LuaFunc fields.
        for (var m : spec.staticMethods()) {
            if (m.isVoid()) continue; // matches existing behavior; void statics are diagnostic-only
            glue.addField(FieldSpec.builder(LuaFunc.class, m.luaName().toUpperCase())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.wrap($T::$L, $S)",
                    LuaFunc.class, m.enclosingType(), m.javaMethodName(), m.luaName())
                .build());
        }

        // Dispatch root instanceof-switch wrapper LuaFunc fields.
        for (var ex : spec.exports()) {
            if (!isRoot(ex)) continue;
            if (!isDispatch(ex)) continue;
            glue.addField(metaWrapperField(upperPrefix(ex, "INDEX"), spec.glueType(), lower(ex.luaName()) + "$index$meta", LuaNames.INDEX_META_NAME));
            glue.addField(metaWrapperField(upperPrefix(ex, "NEWINDEX"), spec.glueType(), lower(ex.luaName()) + "$newindex$meta", LuaNames.NEWINDEX_META_NAME));
            glue.addField(metaWrapperField(upperPrefix(ex, "NAMECALL"), spec.glueType(), lower(ex.luaName()) + "$namecall$meta", LuaNames.NAMECALL_META_NAME));
        }

        // ---------- Methods ----------
        // push / check methods for each root export.
        for (var ex : spec.exports()) {
            if (!isRoot(ex)) continue;
            glue.addMethod(pushMethod(ex));
            glue.addMethod(checkArgMethod(ex));
        }

        // Per-export meta method body wrappers (source order).
        for (var ex : spec.exports()) {
            for (var m : ex.metaMethods()) {
                glue.addMethod(metaMethodBody(ex, m));
            }
        }

        // Per-export dispatch body methods: namecall, newindex, index (in that order).
        for (var ex : spec.exports()) {
            glue.addMethod(namecallDispatchBody(ex));
            glue.addMethod(newIndexDispatchBody(ex));
            glue.addMethod(indexDispatchBody(ex));
        }

        // Dispatch root instanceof-switch methods (index, newindex, namecall — in that order).
        for (var ex : spec.exports()) {
            if (!isRoot(ex)) continue;
            if (!isDispatch(ex)) continue;
            glue.addMethod(instanceofIndexMeta(spec, ex));
            glue.addMethod(instanceofNewIndexMeta(spec, ex));
            glue.addMethod(instanceofNamecallMeta(spec, ex));
        }

        // register()
        glue.addMethod(registerMethod(spec));

        // Top-level meta methods (index, newindex, namecall — in that order).
        glue.addMethod(topIndexMeta(spec));
        glue.addMethod(topNewIndexMeta(spec));
        glue.addMethod(topNamecallMeta(spec));

        return JavaFile.builder(spec.glueType().packageName(), glue.build())
            .addFileComment("Generated by Lua Slopgen. DO NOT EDIT!")
            .indent("    ")
            .build();
    }

    // =====================================================================
    // Field / method helpers
    // =====================================================================

    private static FieldSpec metaWrapperField(String name, ClassName glueType, String methodName, String debugName) {
        return FieldSpec.builder(LuaFunc.class, name)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$T.wrap($T::$L, $S, $T.global())",
                LuaFunc.class, glueType, methodName, debugName, Arena.class)
            .build();
    }

    private static MethodSpec pushMethod(ExportSpec ex) {
        return MethodSpec.methodBuilder("push" + ex.luaName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(NOT_NULL_STATE)
            .addParameter(ex.javaType(), "obj")
            .addStatement("state.newUserDataTaggedWithMetatable(obj, $L)", upperPrefix(ex, "TAG"))
            .build();
    }

    private static MethodSpec checkArgMethod(ExportSpec ex) {
        return MethodSpec.methodBuilder("check" + ex.luaName() + "Arg")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(NOT_NULL_STATE)
            .addParameter(int.class, "argIndex")
            .returns(ex.javaType().annotated(AnnotationSpec.builder(NotNull.class).build()))
            .addStatement("Object obj = state.toUserDataTagged(argIndex, $L)", upperPrefix(ex, "TAG"))
            .beginControlFlow("if (obj instanceof $T actual)", ex.javaType())
            .addStatement("return actual")
            .endControlFlow()
            .addStatement("state.typeError(argIndex, $S)", ex.luaName())
            .addStatement("return null")
            .build();
    }

    private static MethodSpec metaMethodBody(ExportSpec ex, MetaSpec m) {
        var b = MethodSpec.methodBuilder(lower(ex.luaName()) + "$" + m.javaMethodName())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        b.addStatement("$T self = check$LArg(state, 1)", ex.javaType(), ex.luaName());
        b.addStatement("state.remove(1)");
        if (!m.isVoid()) b.addCode("return ");
        b.addStatement("self.$L(state)", m.javaMethodName());
        if (m.isVoid()) b.addStatement("return 0");
        return b.build();
    }

    // ---- per-export dispatch body methods ----

    private MethodSpec namecallDispatchBody(ExportSpec ex) {
        var b = beginDispatchOrMeta(ex, "namecall", "state.nameCallAtomRaw()", /*removeBeforeAtom=*/false);
        for (var m : ex.methods()) {
            short atom = atomTable.atomFor(m.luaName());
            var label = atomResolver.label(m.luaName(), atom);
            b.beginControlFlow("case $L/*$L*/ -> ", label, m.luaName());
            b.addStatement("state.remove(1)");
            if (m.isVoid()) {
                b.addStatement("self.$L(state)", m.javaMethodName());
                b.addStatement("yield 0");
            } else {
                b.addStatement("yield self.$L(state)", m.javaMethodName());
            }
            b.endControlFlow();
        }
        finalizeDispatchSwitch(b, "namecall", ex);
        return b.build();
    }

    private MethodSpec newIndexDispatchBody(ExportSpec ex) {
        var b = beginDispatchOrMeta(ex, "newindex", "state.toStringAtomRaw(1)", /*removeBeforeAtom=*/true);
        for (var p : ex.properties()) {
            if (p.setter() == null) continue;
            short atom = atomTable.atomFor(p.luaName());
            var label = atomResolver.label(p.luaName(), atom);
            b.beginControlFlow("case $L/*$L*/ -> ", label, p.luaName());
            b.addStatement("self.$L(state)", p.setter().javaMethodName());
            b.addStatement("yield 0");
            b.endControlFlow();
        }
        finalizeDispatchSwitch(b, "newindex", ex);
        return b.build();
    }

    private MethodSpec indexDispatchBody(ExportSpec ex) {
        var b = beginDispatchOrMeta(ex, "index", "state.toStringAtomRaw(2)", /*removeBeforeAtom=*/false);
        for (var p : ex.properties()) {
            if (p.getter() == null) continue;
            short atom = atomTable.atomFor(p.luaName());
            var label = atomResolver.label(p.luaName(), atom);
            b.addStatement("case $L/*$L*/ -> self.$L(state)",
                label, p.luaName(), p.getter().javaMethodName());
        }
        finalizeDispatchSwitch(b, "index", ex);
        return b.build();
    }

    private MethodSpec.Builder beginDispatchOrMeta(ExportSpec ex, String kind, String atomReadExpr, boolean removeBeforeAtom) {
        boolean dispatch = isDispatch(ex);
        var name = lower(ex.luaName()) + "$" + kind + "$" + (dispatch ? "dispatch" : "meta");
        var b = MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        if (dispatch) {
            b.addParameter(ex.javaType(), "self");
            b.addParameter(int.class, "atom");
        } else {
            b.addStatement("$T self = check$LArg(state, 1)", ex.javaType(), ex.luaName());
            if (kind.equals("newindex")) {
                b.addStatement("state.remove(1)");
            }
            b.addStatement("short atom = $L", atomReadExpr);
            if (kind.equals("newindex")) {
                b.addStatement("state.remove(1)");
            }
        }
        b.beginControlFlow("return switch (atom)");
        return b;
    }

    private void finalizeDispatchSwitch(MethodSpec.Builder b, String kind, ExportSpec ex) {
        if (kind.equals("index")) {
            b.addStatement("case $T.NO_ATOM -> 0", LuaState.class);
        } else {
            // newindex / namecall: padding so empty switch compiles
            b.addStatement("case Short.MIN_VALUE -> 0");
        }
        if (ex.superExport() != null) {
            var superSimple = simpleNameOf(ex.superExport());
            b.addStatement("default -> $L$$$L$$dispatch(state, self, atom)", lower(superSimple), kind);
        } else {
            if (kind.equals("index")) {
                b.addStatement("default -> 0");
            } else {
                b.addStatement("default -> throw state.error($S)",
                    kind.equals("newindex") ? "Attempt to update nonexistent property" : "Attempt to call nonexistent method");
            }
        }
        b.addCode("$<};\n");
    }

    // ---- instanceof switch (meta) methods for dispatch roots ----

    private MethodSpec instanceofIndexMeta(LibrarySpec spec, ExportSpec root) {
        return instanceofMeta(spec, root, "index", "state.toStringAtomRaw(2)", /*removeBeforeAtom=*/false);
    }

    private MethodSpec instanceofNewIndexMeta(LibrarySpec spec, ExportSpec root) {
        return instanceofMeta(spec, root, "newindex", "state.toStringAtomRaw(1)", /*removeBeforeAtom=*/true);
    }

    private MethodSpec instanceofNamecallMeta(LibrarySpec spec, ExportSpec root) {
        return instanceofMeta(spec, root, "namecall", "state.nameCallAtomRaw()", /*removeBeforeAtom=*/false);
    }

    private MethodSpec instanceofMeta(LibrarySpec spec, ExportSpec root, String kind, String atomReadExpr, boolean removeBeforeAtom) {
        var name = lower(root.luaName()) + "$" + kind + "$meta";
        var b = MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        b.addStatement("$T self = check$LArg(state, 1)", root.javaType(), root.luaName());
        if (removeBeforeAtom) b.addStatement("state.remove(1)");
        b.addStatement("short atom = $L", atomReadExpr);
        if (removeBeforeAtom) b.addStatement("state.remove(1)");
        b.beginControlFlow("return switch (self)");

        // Subtypes of `root` in source order, then reverse so the most-recently-declared is first
        // (matches the original visitor's `.reversed()` over the dispatchedIndex list).
        var descendants = descendantsOf(spec, root);
        java.util.Collections.reverse(descendants);
        for (var sub : descendants) {
            b.addStatement("case $T self1 -> $L$$$L$$dispatch(state, self1, atom)",
                sub.javaType(), lower(sub.luaName()), kind);
        }
        b.addStatement("default -> $L$$$L$$dispatch(state, self, atom)", lower(root.luaName()), kind);
        b.addCode("$<};\n");
        return b.build();
    }

    private static List<ExportSpec> descendantsOf(LibrarySpec spec, ExportSpec root) {
        // Build by-javaType lookup
        var out = new ArrayList<ExportSpec>();
        for (var ex : spec.exports()) {
            if (ex == root) continue;
            // Walk parent chain
            var cur = ex;
            while (cur.superExport() != null) {
                if (cur.superExport().equals(root.javaType())) {
                    out.add(ex);
                    break;
                }
                // climb
                ExportSpec parent = null;
                for (var c : spec.exports()) {
                    if (c.javaType().equals(cur.superExport())) {
                        parent = c;
                        break;
                    }
                }
                if (parent == null) break;
                cur = parent;
            }
        }
        return out;
    }

    // ---- register method ----

    private MethodSpec registerMethod(LibrarySpec spec) {
        var b = MethodSpec.methodBuilder("register")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(LuaState.class, "state");

        // Library-level userdata + metatable
        b.addStatement("state.newUserData(new Object())");
        b.addStatement("state.newMetaTable(LIB_NAME)");
        b.addStatement("state.pushString(LIB_NAME)");
        b.addStatement("state.setField(-2, $S)", "__type");
        b.addStatement("state.pushFunction(INDEX)");
        b.addStatement("state.setField(-2, $S)", LuaNames.INDEX_META_NAME);
        b.addStatement("state.pushFunction(NEWINDEX)");
        b.addStatement("state.setField(-2, $S)", LuaNames.NEWINDEX_META_NAME);
        b.addStatement("state.pushFunction(NAMECALL)");
        b.addStatement("state.setField(-2, $S)", LuaNames.NAMECALL_META_NAME);
        b.addStatement("state.setReadOnly(-1, true)");
        b.addStatement("state.setMetaTable(-2)");
        if (spec.scope() == LuaLibrary.Scope.GLOBAL) {
            b.addStatement("state.setGlobal(LIB_NAME)");
        } else {
            b.addStatement("state.requireRegisterModule(LIB_NAME)");
        }

        // Per-export userdata metatable setup
        for (var ex : spec.exports()) {
            if (!isRoot(ex)) continue;
            b.addCode("\n");
            b.addComment("Type setup for $L", ex.luaName());
            b.addStatement("state.newMetaTable(LIB_NAME + $S)", "." + ex.luaName());
            b.addStatement("state.pushString($S)", ex.luaName());
            b.addStatement("state.setField(-2, $S)", "__type");
            b.addStatement("state.pushFunction($L)", upperPrefix(ex, "INDEX"));
            b.addStatement("state.setField(-2, $S)", LuaNames.INDEX_META_NAME);
            b.addStatement("state.pushFunction($L)", upperPrefix(ex, "NEWINDEX"));
            b.addStatement("state.setField(-2, $S)", LuaNames.NEWINDEX_META_NAME);
            b.addStatement("state.pushFunction($L)", upperPrefix(ex, "NAMECALL"));
            b.addStatement("state.setField(-2, $S)", LuaNames.NAMECALL_META_NAME);
            for (var m : ex.metaMethods()) {
                b.addStatement("state.pushFunction($L)", upperPrefix(ex, m.javaMethodName().toUpperCase()));
                b.addStatement("state.setField(-2, $S)", m.meta().methodName());
            }
            b.addStatement("state.setReadOnly(-1, true)");
            b.addStatement("state.setUserDataMetaTable($L)", upperPrefix(ex, "TAG"));
        }

        return b.build();
    }

    // ---- top-level meta methods ----

    private MethodSpec topIndexMeta(LibrarySpec spec) {
        var b = MethodSpec.methodBuilder("index$meta")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        b.addStatement("short atom = state.toStringAtomRaw(2)");
        b.beginControlFlow("return switch (atom)");

        for (var p : spec.staticProperties()) {
            if (p.getter() == null) continue;
            short atom = atomTable.atomFor(p.luaName());
            var label = atomResolver.label(p.luaName(), atom);
            b.addStatement("case $L/*$L*/ -> $T.$L(state)",
                label, p.luaName(),
                p.getter().enclosingType(),
                p.getter().javaMethodName());
        }
        for (var m : spec.staticMethods()) {
            if (m.isVoid()) continue;
            short atom = atomTable.atomFor(m.luaName());
            var label = atomResolver.label(m.luaName(), atom);
            b.beginControlFlow("case $L/*$L*/ -> ", label, m.luaName());
            b.addStatement("state.pushFunction($L)", m.luaName().toUpperCase());
            b.addStatement("yield 1");
            b.endControlFlow();
        }
        b.addStatement("case $T.NO_ATOM -> 0", LuaState.class);
        b.addStatement("default -> 0");
        b.addCode("$<};\n");
        return b.build();
    }

    private MethodSpec topNewIndexMeta(LibrarySpec spec) {
        var b = MethodSpec.methodBuilder("newindex$meta")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        b.addStatement("state.remove(1)");
        b.addStatement("short atom = state.toStringAtomRaw(1)");
        b.addStatement("state.remove(1)");
        b.beginControlFlow("return switch (atom)");
        b.addStatement("case Short.MIN_VALUE -> 0");
        b.addStatement("default -> throw state.error($S)", "Attempt to update nonexistent property");
        b.addCode("$<};\n");
        return b.build();
    }

    private MethodSpec topNamecallMeta(LibrarySpec spec) {
        var b = MethodSpec.methodBuilder("namecall$meta")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        b.addStatement("short atom = state.nameCallAtomRaw()");
        b.beginControlFlow("return switch (atom)");
        b.addStatement("case Short.MIN_VALUE -> 0");
        b.addStatement("default -> throw state.error($S)", "Attempt to call nonexistent method");
        b.addCode("$<};\n");
        return b.build();
    }

    // =====================================================================
    // Predicates / naming
    // =====================================================================

    private static boolean isDispatch(ExportSpec ex) {
        return ex.superExport() != null || !ex.isFinal();
    }

    private static boolean isRoot(ExportSpec ex) {
        return ex.superExport() == null;
    }

    private static String upperPrefix(ExportSpec ex, String suffix) {
        return ex.luaName().toUpperCase() + "_" + suffix;
    }

    private static String lower(String luaName) {
        return luaName.toLowerCase();
    }

    private static String simpleNameOf(TypeName t) {
        if (t instanceof ClassName c) return c.simpleName();
        return t.toString();
    }
}
