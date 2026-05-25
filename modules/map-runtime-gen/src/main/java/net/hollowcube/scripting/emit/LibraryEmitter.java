package net.hollowcube.scripting.emit;

import com.palantir.javapoet.*;
import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.hollowcube.scripting.Idents;
import net.hollowcube.scripting.LuaNames;
import net.hollowcube.scripting.Model;
import net.hollowcube.scripting.gen.LuaLibrary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.lang.foreign.Arena;
import java.util.*;

/// Translates a [Model.Library] into the generated `<Library>$luau.java` companion class. Field
/// and method ordering matches the original visitor for byte-equivalent output on existing inputs;
/// the emit pass is otherwise free of AST-traversal state.
public final class LibraryEmitter {

    private static final ParameterSpec NOT_NULL_STATE = ParameterSpec.builder(LuaState.class, "state")
        .addAnnotation(NotNull.class)
        .build();

    private final Idents idents;
    private final @Nullable ClassName atomsClass;

    public LibraryEmitter(Idents idents, @Nullable ClassName atomsClass) {
        this.idents = idents;
        this.atomsClass = atomsClass;
    }

    public JavaFile emit(Model.Library library) {
        return emit(library, new Element[0]);
    }

    /// Emit a library with originating elements attached to the generated TypeSpec. Gradle uses
    /// the originating elements to associate the generated source with its input file — without
    /// them, isolating annotation processing falls back to non-incremental.
    public JavaFile emit(Model.Library library, Element... originatingElements) {
        var glue = TypeSpec.classBuilder(library.glueType())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        for (var e : originatingElements) glue.addOriginatingElement(e);

        // ---------- Fields ----------
        glue.addField(FieldSpec.builder(String.class, "LIB_NAME")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", library.moduleName())
            .build());
        glue.addField(metaWrapperField("INDEX", library.glueType(), "index$meta", LuaNames.INDEX_META_NAME));
        glue.addField(metaWrapperField("NEWINDEX", library.glueType(), "newindex$meta", LuaNames.NEWINDEX_META_NAME));
        glue.addField(metaWrapperField("NAMECALL", library.glueType(), "namecall$meta", LuaNames.NAMECALL_META_NAME));

        // Per-export header fields. For final/leaf exports the meta wrapper LuaFunc fields are
        // emitted up-front (these wrap the body method directly). For dispatch roots they are
        // emitted later, after the body methods exist.
        for (var ex : library.exports()) {
            if (!isRoot(ex)) continue;

            if (!isDispatch(ex)) {
                glue.addField(metaWrapperField(upperPrefix(ex, "INDEX"), library.glueType(), lower(ex.luaName()) + "$index$meta", LuaNames.INDEX_META_NAME));
                glue.addField(metaWrapperField(upperPrefix(ex, "NEWINDEX"), library.glueType(), lower(ex.luaName()) + "$newindex$meta", LuaNames.NEWINDEX_META_NAME));
                glue.addField(metaWrapperField(upperPrefix(ex, "NAMECALL"), library.glueType(), lower(ex.luaName()) + "$namecall$meta", LuaNames.NAMECALL_META_NAME));
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
                    library.glueType(),
                    lower(ex.luaName()) + "$" + m.javaMethodName(),
                    m.meta()));
            }
        }

        // Inner-enum static fields: light-userdata tag + Java values array. Order is per-enum
        // source declaration so atom assignment downstream stays deterministic. Java identifier
        // names use the enum's Java simple name (matches the `pushItem`/`checkItemArg`
        // convention); only the Lua-visible identifiers carry the `@LuaEnum(name)` override.
        for (var en : library.enums()) {
            String javaName = en.sourceType().simpleName().toUpperCase();
            glue.addField(FieldSpec.builder(int.class, javaName + "_TAG")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", en.lightUserDataTag())
                .build());
            glue.addField(FieldSpec.builder(
                    com.palantir.javapoet.ArrayTypeName.of(en.sourceType()),
                    javaName + "_VALUES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.values()", en.sourceType())
                .build());
        }

        // Static method LuaFunc fields.
        for (var m : library.staticMethods()) {
            if (m.isVoid()) continue; // matches existing behavior; void statics are diagnostic-only
            glue.addField(FieldSpec.builder(LuaFunc.class, m.luaName().toUpperCase())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.wrap($T::$L, $S)",
                    LuaFunc.class, m.enclosingType(), m.javaMethodName(), m.luaName())
                .build());
        }

        // Dispatch root instanceof-switch wrapper LuaFunc fields.
        for (var ex : library.exports()) {
            if (!isRoot(ex)) continue;
            if (!isDispatch(ex)) continue;
            glue.addField(metaWrapperField(upperPrefix(ex, "INDEX"), library.glueType(), lower(ex.luaName()) + "$index$meta", LuaNames.INDEX_META_NAME));
            glue.addField(metaWrapperField(upperPrefix(ex, "NEWINDEX"), library.glueType(), lower(ex.luaName()) + "$newindex$meta", LuaNames.NEWINDEX_META_NAME));
            glue.addField(metaWrapperField(upperPrefix(ex, "NAMECALL"), library.glueType(), lower(ex.luaName()) + "$namecall$meta", LuaNames.NAMECALL_META_NAME));
        }

        // ---------- Methods ----------
        // push / check methods for each root export.
        for (var ex : library.exports()) {
            if (!isRoot(ex)) continue;
            glue.addMethod(pushMethod(ex));
            glue.addMethod(checkArgMethod(ex));
        }

        // push / check methods for each inner enum.
        for (var en : library.enums()) {
            glue.addMethod(enumPushMethod(en));
            glue.addMethod(enumCheckArgMethod(en));
        }

        // Per-export meta method body wrappers (source order).
        for (var ex : library.exports()) {
            for (var m : ex.metaMethods()) {
                glue.addMethod(metaMethodBody(ex, m));
            }
        }

        // Per-export dispatch body methods: namecall, newindex, index (in that order).
        for (var ex : library.exports()) {
            glue.addMethod(namecallDispatchBody(ex));
            glue.addMethod(newIndexDispatchBody(ex));
            glue.addMethod(indexDispatchBody(ex));
        }

        // Dispatch root instanceof-switch methods (index, newindex, namecall — in that order).
        for (var ex : library.exports()) {
            if (!isRoot(ex)) continue;
            if (!isDispatch(ex)) continue;
            glue.addMethod(instanceofIndexMeta(library, ex));
            glue.addMethod(instanceofNewIndexMeta(library, ex));
            glue.addMethod(instanceofNamecallMeta(library, ex));
        }

        // register()
        glue.addMethod(registerMethod(library));

        // Top-level meta methods (index, newindex, namecall — in that order).
        glue.addMethod(topIndexMeta(library));
        glue.addMethod(topNewIndexMeta(library));
        glue.addMethod(topNamecallMeta(library));

        return JavaFile.builder(library.glueType().packageName(), glue.build())
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

    private static MethodSpec pushMethod(Model.Export ex) {
        return MethodSpec.methodBuilder("push" + ex.luaName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(NOT_NULL_STATE)
            .addParameter(ex.javaType(), "obj")
            .addStatement("state.newUserDataTaggedWithMetatable(obj, $L)", upperPrefix(ex, "TAG"))
            .build();
    }

    private static MethodSpec checkArgMethod(Model.Export ex) {
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

    /// `pushLuaSlot(state, LuaSlot value)` — pushes a tagged light userdata carrying the
    /// enum's ordinal. Method name tracks the Java simple name; the userdata tag identifier
    /// uses the same upper-cased Java name.
    private static MethodSpec enumPushMethod(Model.EnumDecl en) {
        String javaName = en.sourceType().simpleName();
        return MethodSpec.methodBuilder("push" + javaName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(NOT_NULL_STATE)
            .addParameter(en.sourceType(), "value")
            .addStatement("state.pushLightUserDataTagged(value.ordinal(), $L_TAG)",
                javaName.toUpperCase())
            .build();
    }

    /// `checkLuaSlotArg(state, idx)` — verifies the tag matches and looks the value back up by
    /// ordinal. The runtime error string uses the Lua-visible name so script authors see the
    /// label they wrote; everything else uses the Java name.
    private static MethodSpec enumCheckArgMethod(Model.EnumDecl en) {
        String javaName = en.sourceType().simpleName();
        return MethodSpec.methodBuilder("check" + javaName + "Arg")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(NOT_NULL_STATE)
            .addParameter(int.class, "argIndex")
            .returns(en.sourceType().annotated(AnnotationSpec.builder(NotNull.class).build()))
            .beginControlFlow("if ($L_TAG != state.lightUserDataTag(argIndex))",
                javaName.toUpperCase())
            .addStatement("state.typeError(argIndex, $S)", en.luaName())
            .addStatement("return null")
            .endControlFlow()
            .addStatement("return $L_VALUES[(int) state.toLightUserData(argIndex)]",
                javaName.toUpperCase())
            .build();
    }

    private static MethodSpec metaMethodBody(Model.Export ex, Model.MetaMethod m) {
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

    private MethodSpec namecallDispatchBody(Model.Export ex) {
        var b = beginDispatchOrMeta(ex, "namecall", "state.nameCallAtomRaw()", /*removeBeforeAtom=*/false);
        for (var m : ex.methods()) {
            short atom = idents.atomFor(m.luaName());
            var label = LuaNames.atomLabel(atomsClass, m.luaName(), atom);
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

    private MethodSpec newIndexDispatchBody(Model.Export ex) {
        var b = beginDispatchOrMeta(ex, "newindex", "state.toStringAtomRaw(1)", /*removeBeforeAtom=*/true);
        for (var p : ex.properties()) {
            if (p.setter() == null) continue;
            short atom = idents.atomFor(p.luaName());
            var label = LuaNames.atomLabel(atomsClass, p.luaName(), atom);
            b.beginControlFlow("case $L/*$L*/ -> ", label, p.luaName());
            b.addStatement("self.$L(state)", p.setter().javaMethodName());
            b.addStatement("yield 0");
            b.endControlFlow();
        }
        finalizeDispatchSwitch(b, "newindex", ex);
        return b.build();
    }

    private MethodSpec indexDispatchBody(Model.Export ex) {
        var b = beginDispatchOrMeta(ex, "index", "state.toStringAtomRaw(2)", /*removeBeforeAtom=*/false);
        for (var p : ex.properties()) {
            if (p.getter() == null) continue;
            short atom = idents.atomFor(p.luaName());
            var label = LuaNames.atomLabel(atomsClass, p.luaName(), atom);
            b.addStatement("case $L/*$L*/ -> self.$L(state)",
                label, p.luaName(), p.getter().javaMethodName());
        }
        finalizeDispatchSwitch(b, "index", ex);
        return b.build();
    }

    private MethodSpec.Builder beginDispatchOrMeta(Model.Export ex, String kind, String atomReadExpr, boolean removeBeforeAtom) {
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

    private void finalizeDispatchSwitch(MethodSpec.Builder b, String kind, Model.Export ex) {
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

    private MethodSpec instanceofIndexMeta(Model.Library library, Model.Export root) {
        return instanceofMeta(library, root, "index", "state.toStringAtomRaw(2)", /*removeBeforeAtom=*/false);
    }

    private MethodSpec instanceofNewIndexMeta(Model.Library library, Model.Export root) {
        return instanceofMeta(library, root, "newindex", "state.toStringAtomRaw(1)", /*removeBeforeAtom=*/true);
    }

    private MethodSpec instanceofNamecallMeta(Model.Library library, Model.Export root) {
        return instanceofMeta(library, root, "namecall", "state.nameCallAtomRaw()", /*removeBeforeAtom=*/false);
    }

    private MethodSpec instanceofMeta(Model.Library library, Model.Export root, String kind, String atomReadExpr, boolean removeBeforeAtom) {
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

        for (var sub : descendantsOf(library, root)) {
            b.addStatement("case $T self1 -> $L$$$L$$dispatch(state, self1, atom)",
                sub.javaType(), lower(sub.luaName()), kind);
        }
        b.addStatement("default -> $L$$$L$$dispatch(state, self, atom)", lower(root.luaName()), kind);
        b.addCode("$<};\n");
        return b.build();
    }

    /// All exports that transitively inherit from `root`, ordered so that subtypes always
    /// precede their supertypes — required for the generated `switch (self)` because Java
    /// rejects later case labels dominated by an earlier supertype label.
    ///
    /// Sort key is inheritance depth (descending), with a stable secondary by source order so
    /// the output is deterministic across AP rounds even when `library.exports()` arrives
    /// in a different order. Using source order alone (the previous implementation) was a
    /// fragile proxy that broke whenever incremental compilation surfaced exports out of
    /// declaration order.
    private static List<Model.Export> descendantsOf(Model.Library library, Model.Export root) {
        var byJavaType = new HashMap<TypeName, Model.Export>();
        for (var ex : library.exports()) byJavaType.put(ex.javaType(), ex);

        var sourceIndex = new HashMap<TypeName, Integer>();
        for (int i = 0; i < library.exports().size(); i++)
            sourceIndex.put(library.exports().get(i).javaType(), i);

        var descendants = new ArrayList<Model.Export>();
        for (var ex : library.exports()) {
            if (ex == root) continue;
            var cur = ex;
            while (cur.superExport() != null) {
                if (cur.superExport().equals(root.javaType())) {
                    descendants.add(ex);
                    break;
                }
                cur = byJavaType.get(cur.superExport());
                if (cur == null) break;
            }
        }

        descendants.sort(Comparator
            .comparingInt((Model.Export e) -> -depthFromRoot(e, root, byJavaType))
            .thenComparingInt(e -> sourceIndex.getOrDefault(e.javaType(), Integer.MAX_VALUE)));
        return descendants;
    }

    /// Number of `superExport` hops from `e` up to `root`. `root` itself is depth 0; a direct
    /// child is 1; grandchild 2, etc. Returns `Integer.MIN_VALUE` if `e` is not actually a
    /// descendant of `root` (caller should already have filtered, so this is defensive only).
    private static int depthFromRoot(Model.Export e, Model.Export root, Map<TypeName, Model.Export> byJavaType) {
        int depth = 0;
        var cur = e;
        while (cur != null && !cur.javaType().equals(root.javaType())) {
            if (cur.superExport() == null) return Integer.MIN_VALUE;
            cur = byJavaType.get(cur.superExport());
            depth++;
        }
        return depth;
    }

    // ---- register method ----

    private MethodSpec registerMethod(Model.Library library) {
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
        // Inner enums: build each constant table as a field on the library metatable. The index
        // metamethod fetches them from here at access time — keeping the table identity stable
        // across accesses so `Lib.Slot == Lib.Slot` holds.
        for (var en : library.enums()) {
            b.addCode("\n");
            b.addComment("Enum table for $L", en.luaName());
            b.addStatement("state.newTable()");
            String tagBase = en.sourceType().simpleName().toUpperCase();
            for (var c : en.constants()) {
                b.addStatement("state.pushLightUserDataTagged($L, $L_TAG)",
                    c.ordinal(), tagBase);
                b.addStatement("state.setField(-2, $S)", c.luaName());
            }
            b.addStatement("state.setReadOnly(-1, true)");
            b.addStatement("state.setField(-2, $S)", en.luaName());
        }
        b.addStatement("state.setReadOnly(-1, true)");
        b.addStatement("state.setMetaTable(-2)");
        if (library.scope() == LuaLibrary.Scope.GLOBAL) {
            b.addStatement("state.setGlobal(LIB_NAME)");
        } else {
            b.addStatement("state.requireRegisterModule(LIB_NAME)");
        }

        // Per-export userdata metatable setup
        for (var ex : library.exports()) {
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
                b.addStatement("state.setField(-2, $S)", m.meta());
            }
            b.addStatement("state.setReadOnly(-1, true)");
            b.addStatement("state.setUserDataMetaTable($L)", upperPrefix(ex, "TAG"));
        }

        return b.build();
    }

    // ---- top-level meta methods ----

    private MethodSpec topIndexMeta(Model.Library library) {
        var b = MethodSpec.methodBuilder("index$meta")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        b.addStatement("short atom = state.toStringAtomRaw(2)");
        b.beginControlFlow("return switch (atom)");

        for (var p : library.staticProperties()) {
            if (p.getter() == null) continue;
            short atom = idents.atomFor(p.luaName());
            var label = LuaNames.atomLabel(atomsClass, p.luaName(), atom);
            b.addStatement("case $L/*$L*/ -> $T.$L(state)",
                label, p.luaName(),
                p.getter().enclosingType(),
                p.getter().javaMethodName());
        }
        for (var m : library.staticMethods()) {
            if (m.isVoid()) continue;
            short atom = idents.atomFor(m.luaName());
            var label = LuaNames.atomLabel(atomsClass, m.luaName(), atom);
            b.beginControlFlow("case $L/*$L*/ -> ", label, m.luaName());
            b.addStatement("state.pushFunction($L)", m.luaName().toUpperCase());
            b.addStatement("yield 1");
            b.endControlFlow();
        }
        // Inner enums: their constant tables live on the library's metatable. Fetch via
        // `getMetaTable(1)` → `getField(-1, "<EnumName>")` → drop the metatable. Three ops per
        // access, the table identity is stable (built once at register time).
        for (var en : library.enums()) {
            short atom = idents.atomFor(en.luaName());
            var label = LuaNames.atomLabel(atomsClass, en.luaName(), atom);
            b.beginControlFlow("case $L/*$L*/ -> ", label, en.luaName());
            b.addStatement("state.getMetaTable(1)");
            b.addStatement("state.getField(-1, $S)", en.luaName());
            b.addStatement("state.remove(-2)");
            b.addStatement("yield 1");
            b.endControlFlow();
        }
        b.addStatement("case $T.NO_ATOM -> 0", LuaState.class);
        b.addStatement("default -> 0");
        b.addCode("$<};\n");
        return b.build();
    }

    private MethodSpec topNewIndexMeta(Model.Library library) {
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

    private MethodSpec topNamecallMeta(Model.Library library) {
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

    private static boolean isDispatch(Model.Export ex) {
        return ex.superExport() != null || !ex.isFinal();
    }

    private static boolean isRoot(Model.Export ex) {
        return ex.superExport() == null;
    }

    private static String upperPrefix(Model.Export ex, String suffix) {
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
