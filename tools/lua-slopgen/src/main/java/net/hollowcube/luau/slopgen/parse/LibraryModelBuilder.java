package net.hollowcube.luau.slopgen.parse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.gen.*;
import net.hollowcube.luau.slopgen.Idents;
import net.hollowcube.luau.slopgen.LuaNames;
import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.docs.*;
import net.hollowcube.luau.slopgen.types.LuauParseException;
import net.hollowcube.luau.slopgen.types.LuauType;
import net.hollowcube.luau.slopgen.types.LuauTypeParser;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.*;

/// Walks a `@LuaLibrary`-annotated class and produces an immutable [Model.Library]. Atom and
/// userdata-tag assignments are accumulated into the shared [Idents] provided by the caller —
/// those are later consumed by the atom-table emitter.
///
/// Type expressions on `@luaParam` / `@luaReturn` tags are parsed into [LuauType] AST here;
/// downstream consumers (codegen, JSON emit, diff) work off the AST directly. Parse failures are
/// reported via the processor `Messager`.
public final class LibraryModelBuilder {
    private final ProcessingEnvironment env;
    private final Idents idents;
    private final LuaDocsValidator docsValidator;

    public LibraryModelBuilder(ProcessingEnvironment env, Idents idents) {
        this.env = env;
        this.idents = idents;
        this.docsValidator = new LuaDocsValidator(env.getMessager());
    }

    public @Nullable Model.Library build(TypeElement libraryClass) {
        var luaLibrary = libraryClass.getAnnotation(LuaLibrary.class);
        if (luaLibrary == null) return null;

        var packageName = env.getElementUtils().getPackageOf(libraryClass).getQualifiedName().toString();
        var sourceType = ClassName.get(packageName, libraryClass.getSimpleName().toString());
        var glueType = ClassName.get(packageName, libraryClass.getSimpleName() + "$luau");

        var libraryDocs = parseDocs(libraryClass);
        docsValidator.validateLibraryContainer(libraryClass, libraryDocs);

        var staticGetters = new LinkedHashMap<String, Model.Accessor>();
        var staticSetters = new LinkedHashMap<String, Model.Accessor>();
        var staticGetterDocs = new LinkedHashMap<String, MemberDocs>();
        var staticSetterDocs = new LinkedHashMap<String, MemberDocs>();
        var staticMethods = new ArrayList<Model.Method>();
        var rawExports = new ArrayList<Model.Export>();

        for (var member : libraryClass.getEnclosedElements()) {
            if (member instanceof TypeElement nested) {
                if (nested.getAnnotation(LuaExport.class) != null)
                    rawExports.add(buildExport(nested));
                continue;
            }
            if (!(member instanceof ExecutableElement method)) continue;

            var luaProperty = method.getAnnotation(LuaProperty.class);
            var luaMethod = method.getAnnotation(LuaMethod.class);
            if (luaProperty == null && luaMethod == null) continue;

            if (!method.getModifiers().contains(Modifier.STATIC)) {
                env.getMessager().printError("Only static methods can be exported from library classes", method);
                continue;
            }

            var javaName = method.getSimpleName().toString();
            var enclosingType = TypeName.get(method.getEnclosingElement().asType());
            var memberDocs = parseDocs(method);

            if (luaProperty != null) {
                var luaName = LuaNames.toLuaProperty(javaName);
                if (javaName.startsWith("set")) {
                    docsValidator.validateSetter(method, memberDocs);
                    staticSetters.put(luaName, buildSetter(method, javaName, enclosingType, memberDocs));
                    staticSetterDocs.put(luaName, memberDocs);
                } else {
                    docsValidator.validateGetter(method, memberDocs);
                    staticGetters.put(luaName, buildGetter(method, javaName, enclosingType, memberDocs));
                    staticGetterDocs.put(luaName, memberDocs);
                }
            } else {
                var luaName = LuaNames.toLuaMethod(javaName);
                var isVoid = method.getReturnType().getKind() == TypeKind.VOID;
                docsValidator.validateMethod(method, memberDocs, isVoid);
                staticMethods.add(buildMethod(method, luaName, javaName, isVoid, enclosingType, memberDocs));
            }
        }

        var staticProperties = mergeAccessors(staticGetters, staticSetters);
        validateAccessorPairing(staticProperties, staticGetterDocs, staticSetterDocs, libraryClass);

        // Reserve atoms in source-declaration order so the emitter's literal references match.
        for (var p : staticProperties) idents.atomFor(p.luaName());
        for (var m : staticMethods) idents.atomFor(m.luaName());

        var supersUsed = new HashSet<TypeName>();
        for (var ex : rawExports)
            if (ex.superExport() != null) supersUsed.add(ex.superExport());

        var finalizedExports = new ArrayList<Model.Export>(rawExports.size());
        for (var ex : rawExports)
            finalizedExports.add(ex.withSubtypes(supersUsed.contains(ex.javaType())));

        return new Model.Library(
            sourceType, glueType, luaLibrary.name(), luaLibrary.scope(),
            finalizedExports, staticMethods, staticProperties, libraryDocs.description());
    }

    private Model.Export buildExport(TypeElement exportClass) {
        var javaType = TypeName.get(exportClass.asType());
        var luaName = exportClass.getSimpleName().toString();
        boolean isFinal = exportClass.getModifiers().contains(Modifier.FINAL);

        var exportDocs = parseDocs(exportClass);
        docsValidator.validateExportContainer(exportClass, exportDocs);

        // Type-level generics — shared by every method, accessor, and meta-method on this
        // export. Method-level `@luaGeneric` may not redeclare any of these names; the check
        // happens per-member below.
        var typeGenerics = mapGenerics(exportDocs.generics());
        var typeGenericNames = new HashSet<String>();
        for (var g : typeGenerics) typeGenericNames.add(g.name());

        TypeName superExport = null;
        var superMirror = exportClass.getSuperclass();
        if (superMirror.getKind() != TypeKind.NONE) {
            var superTypeName = TypeName.get(superMirror);
            if (!superTypeName.equals(TypeName.get(Object.class))
                && !superTypeName.equals(TypeName.get(Record.class)))
                superExport = superTypeName;
        }

        int tag = idents.allocUserDataTag();

        var getters = new LinkedHashMap<String, Model.Accessor>();
        var setters = new LinkedHashMap<String, Model.Accessor>();
        var getterDocs = new LinkedHashMap<String, MemberDocs>();
        var setterDocs = new LinkedHashMap<String, MemberDocs>();
        var methods = new ArrayList<Model.Method>();
        var metaMethods = new ArrayList<Model.MetaMethod>();

        for (var member : exportClass.getEnclosedElements()) {
            if (!(member instanceof ExecutableElement method)) continue;

            var luaProperty = method.getAnnotation(LuaProperty.class);
            var luaMethod = method.getAnnotation(LuaMethod.class);
            if (luaProperty == null && luaMethod == null) continue;

            if (method.getModifiers().contains(Modifier.STATIC)) {
                env.getMessager().printError("Only non-static methods can be exported from exported type classes", method);
                continue;
            }

            var javaName = method.getSimpleName().toString();
            var isVoid = method.getReturnType().getKind() == TypeKind.VOID;
            var memberDocs = parseDocs(method);

            // Method-level `@luaGeneric T` shadowing a type-level `T` is rejected — the same
            // name would denote two different things in `(A...) -> T` style signatures.
            checkGenericShadowing(method, memberDocs, typeGenericNames, luaName);

            if (luaProperty != null) {
                var name = LuaNames.toLuaProperty(javaName);
                if (javaName.startsWith("set")) {
                    docsValidator.validateSetter(method, memberDocs);
                    setters.put(name, buildSetter(method, javaName, javaType, memberDocs));
                    setterDocs.put(name, memberDocs);
                } else {
                    docsValidator.validateGetter(method, memberDocs);
                    getters.put(name, buildGetter(method, javaName, javaType, memberDocs));
                    getterDocs.put(name, memberDocs);
                }
            } else if (luaMethod.meta() != Meta.NONE) {
                docsValidator.validateMeta(method, memberDocs, isVoid);
                metaMethods.add(buildMetaMethod(method, luaMethod.meta(), javaName, isVoid, memberDocs));
            } else {
                docsValidator.validateMethod(method, memberDocs, isVoid);
                var name = LuaNames.toLuaMethod(javaName);
                methods.add(buildMethod(method, name, javaName, isVoid, javaType, memberDocs));
            }
        }

        var properties = mergeAccessors(getters, setters);
        validateAccessorPairing(properties, getterDocs, setterDocs, exportClass);

        for (var p : properties) idents.atomFor(p.luaName());
        for (var m : methods) idents.atomFor(m.luaName());

        return new Model.Export(javaType, luaName, superExport, isFinal,
            typeGenerics, properties, methods, metaMethods, tag,
            /*hasSubtypes=*/false, exportDocs.description());
    }

    /// Reject method-level `@luaGeneric` declarations that reuse a name already bound by an
    /// `@luaGeneric` on the enclosing `@LuaExport` class. The type-level generic is already
    /// in scope for every member; redeclaring it on a single method would be ambiguous.
    private void checkGenericShadowing(
        ExecutableElement method, MemberDocs docs, Set<String> typeGenericNames, String typeLuaName
    ) {
        if (typeGenericNames.isEmpty()) return;
        for (var g : docs.generics()) {
            if (typeGenericNames.contains(g.name())) {
                env.getMessager().printError(
                    "@luaGeneric " + g.name() + " shadows the same name on enclosing type '"
                    + typeLuaName + "' — remove the redeclaration on the method, the type-level "
                    + "generic is already in scope.",
                    method);
            }
        }
    }

    private Model.Method buildMethod(
        ExecutableElement source, String luaName, String javaName, boolean isVoid,
        TypeName enclosingType, MemberDocs docs
    ) {
        return new Model.Method(
            luaName, javaName, isVoid, enclosingType,
            docs.description(),
            mapGenerics(docs.generics()),
            mapParams(source, docs.params()),
            mapReturns(source, docs.returns()));
    }

    private Model.MetaMethod buildMetaMethod(
        ExecutableElement source, Meta meta, String javaName, boolean isVoid, MemberDocs docs
    ) {
        return new Model.MetaMethod(
            meta, javaName, isVoid,
            docs.description(),
            mapGenerics(docs.generics()),
            mapParams(source, docs.params()),
            mapReturns(source, docs.returns()));
    }

    private Model.Accessor buildGetter(
        ExecutableElement source, String javaName, TypeName enclosingType, MemberDocs docs
    ) {
        var firstReturn = docs.returns().isEmpty() ? null : docs.returns().get(0);
        LuauType type = parseTypeOrNil(source, firstReturn == null ? "" : firstReturn.typeExpr());
        return new Model.Accessor(javaName, enclosingType, docs.description(), null, type);
    }

    private Model.Accessor buildSetter(
        ExecutableElement source, String javaName, TypeName enclosingType, MemberDocs docs
    ) {
        String paramName = "";
        String typeExpr = "";
        if (!docs.params().isEmpty()) {
            var p = docs.params().get(0);
            paramName = p.name();
            typeExpr = p.typeExpr();
        }
        LuauType type = parseTypeOrNil(source, typeExpr);
        return new Model.Accessor(javaName, enclosingType, docs.description(), paramName, type);
    }

    private List<Model.GenericParam> mapGenerics(List<TagGeneric> generics) {
        var out = new ArrayList<Model.GenericParam>(generics.size());
        for (var g : generics) out.add(new Model.GenericParam(g.name(), g.pack(), g.description()));
        return out;
    }

    private List<Model.Param> mapParams(ExecutableElement source, List<TagParam> params) {
        var out = new ArrayList<Model.Param>(params.size());
        for (var p : params) out.add(new Model.Param(
            p.name(), p.optional(), parseTypeOrNil(source, p.typeExpr()), p.description()));
        return out;
    }

    private List<Model.Return> mapReturns(ExecutableElement source, List<TagReturn> returns) {
        var out = new ArrayList<Model.Return>(returns.size());
        for (var r : returns) out.add(new Model.Return(parseTypeOrNil(source, r.typeExpr()), r.description()));
        return out;
    }

    /// Parse a type expression to AST. Empty input is mapped to `nil` so that callers downstream
    /// don't need null checks; an unparseable expression produces a diagnostic pinned to the
    /// source element and a `nil` placeholder so the rest of the build can continue.
    ///
    /// Severity tracks [LuaDocsValidator#STRICT] — while the codebase is being migrated to clean
    /// `@luaReturn`/`@luaParam` types, parse failures are warnings (the published JSON contains
    /// `nil` placeholders for them).
    private LuauType parseTypeOrNil(Element source, String typeExpr) {
        if (typeExpr == null || typeExpr.isBlank()) return new LuauType.Named(null, "nil", List.of());
        try {
            return LuauTypeParser.parse(typeExpr);
        } catch (LuauParseException e) {
            var severity = LuaDocsValidator.STRICT ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
            env.getMessager().printMessage(severity,
                "Could not parse Luau type '" + typeExpr + "': " + e.getMessage(), source);
            return new LuauType.Named(null, "nil", List.of());
        }
    }

    private MemberDocs parseDocs(Element element) {
        return JavadocTagParser.parse(env.getElementUtils().getDocComment(element));
    }

    /// Cross-check getter/setter type expressions on a merged property list. Reports against
    /// the setter element so authors see the error at the side that's typically the reactive
    /// edit (you usually add a setter to an existing typed getter).
    private void validateAccessorPairing(
        List<Model.Property> properties,
        Map<String, MemberDocs> getterDocs,
        Map<String, MemberDocs> setterDocs,
        Element fallback
    ) {
        for (var p : properties) {
            if (p.getter() == null || p.setter() == null) continue;
            var g = getterDocs.get(p.luaName());
            var s = setterDocs.get(p.luaName());
            if (g == null || s == null) continue;
            // The validator wants the actual setter element for IDE pinning. We don't have it
            // directly here, so re-locate via the enclosing element. In practice both accessors
            // belong to the same enclosing class, so attaching to `fallback` is acceptable until
            // we plumb the original elements through; the message text identifies the property.
            docsValidator.validatePropertyConsistency(fallback, g, s);
        }
    }

    private static List<Model.Property> mergeAccessors(
        LinkedHashMap<String, Model.Accessor> getters,
        LinkedHashMap<String, Model.Accessor> setters
    ) {
        var seen = new LinkedHashSet<String>();
        seen.addAll(getters.keySet());
        seen.addAll(setters.keySet());
        var out = new ArrayList<Model.Property>(seen.size());
        for (var name : seen)
            out.add(new Model.Property(name, getters.get(name), setters.get(name)));
        return out;
    }
}
