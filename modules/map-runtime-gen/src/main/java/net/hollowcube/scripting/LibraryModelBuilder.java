package net.hollowcube.scripting;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.scripting.docs.DocTag;
import net.hollowcube.scripting.docs.Docs;
import net.hollowcube.scripting.docs.JavadocTagParser;
import net.hollowcube.scripting.docs.LuaDocsValidator;
import net.hollowcube.scripting.gen.*;
import net.hollowcube.scripting.types.LuauParseException;
import net.hollowcube.scripting.types.LuauType;
import net.hollowcube.scripting.types.LuauTypeParser;
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
        var staticMethods = new ArrayList<Model.Method>();
        var rawExports = new ArrayList<Model.Export>();
        var rawExportElements = new ArrayList<TypeElement>();
        var rawEnums = new ArrayList<Model.EnumDecl>();

        for (var member : libraryClass.getEnclosedElements()) {
            if (member instanceof TypeElement nested) {
                if (nested.getAnnotation(LuaExport.class) != null) {
                    rawExports.add(buildExport(nested));
                    rawExportElements.add(nested);
                }
                if (nested.getAnnotation(LuaEnum.class) != null) {
                    var ed = EnumModelBuilder.build(env, idents, nested, glueType, luaLibrary.scope());
                    if (ed != null) rawEnums.add(ed);
                }
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
                } else {
                    docsValidator.validateGetter(method, memberDocs);
                    staticGetters.put(luaName, buildGetter(method, javaName, enclosingType, memberDocs));
                }
            } else {
                var luaName = LuaNames.toLuaMethod(javaName);
                var isVoid = method.getReturnType().getKind() == TypeKind.VOID;
                docsValidator.validateMethod(method, memberDocs, isVoid);
                staticMethods.add(buildMethod(method, luaName, javaName, isVoid, enclosingType, memberDocs));
            }
        }

        var staticProperties = mergeAccessors(staticGetters, staticSetters);

        // Reserve atoms in source-declaration order so the emitter's literal references match.
        for (var p : staticProperties) idents.atomFor(p.luaName());
        for (var m : staticMethods) idents.atomFor(m.luaName());
        for (var en : rawEnums) idents.atomFor(en.luaName());

        applyUnionMetadata(rawExports, rawExportElements);

        var supersUsed = new HashSet<TypeName>();
        for (var ex : rawExports)
            if (ex.superExport() != null) supersUsed.add(ex.superExport());

        var finalizedExports = new ArrayList<Model.Export>(rawExports.size());
        for (var ex : rawExports)
            finalizedExports.add(ex.withSubtypes(supersUsed.contains(ex.javaType())));

        return new Model.Library(
            sourceType, glueType, luaLibrary.name(), luaLibrary.scope(),
            finalizedExports, rawEnums, staticMethods, staticProperties, libraryDocs.description());
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
                } else {
                    docsValidator.validateGetter(method, memberDocs);
                    getters.put(name, buildGetter(method, javaName, javaType, memberDocs));
                }
            } else if (!luaMethod.meta().isEmpty()) {
                docsValidator.validateMeta(method, memberDocs, isVoid);
                metaMethods.add(buildMetaMethod(method, luaMethod.meta(), javaName, isVoid, memberDocs));
            } else {
                docsValidator.validateMethod(method, memberDocs, isVoid);
                var name = LuaNames.toLuaMethod(javaName);
                methods.add(buildMethod(method, name, javaName, isVoid, javaType, memberDocs));
            }
        }

        var properties = mergeAccessors(getters, setters);

        for (var p : properties) idents.atomFor(p.luaName());
        for (var m : methods) idents.atomFor(m.luaName());

        return new Model.Export(javaType, luaName, superExport, isFinal,
            typeGenerics, properties, methods, metaMethods, tag,
            /*hasSubtypes=*/false,
            Model.Export.Kind.STRUCT, List.of(), null,
            exportDocs.description());
    }

    /// Post-pass: stamp each `@LuaUnion`-marked export and its permitted variants with the
    /// correct [Model.Export.Kind]. Validates the union contract (sealed + abstract parent,
    /// every permitted type also `@LuaExport`, every variant `extends` the parent directly) and
    /// — when `@LuaUnion#discriminator` is set — the per-variant discriminator property.
    ///
    /// Mutates `rawExports` in-place, replacing entries with their union-stamped counterparts.
    /// Validation diagnostics are printed against the offending element so authors land on the
    /// right line in their editor.
    private void applyUnionMetadata(List<Model.Export> rawExports, List<TypeElement> exportElements) {
        // First pass: collect union parents.
        var byJavaType = new HashMap<TypeName, Integer>();
        for (int i = 0; i < rawExports.size(); i++)
            byJavaType.put(rawExports.get(i).javaType(), i);

        for (int parentIdx = 0; parentIdx < rawExports.size(); parentIdx++) {
            var parentElement = exportElements.get(parentIdx);
            var luaUnion = parentElement.getAnnotation(LuaUnion.class);
            if (luaUnion == null) continue;

            // Parent must be sealed + abstract. Sealed gives us a deterministic variant list;
            // abstract enforces that you can only construct the variants (the alias itself has
            // no runtime representation).
            var mods = parentElement.getModifiers();
            if (!mods.contains(Modifier.SEALED)) {
                env.getMessager().printError(
                    "@LuaUnion requires the class to be `sealed` so its variant set is closed",
                    parentElement);
                continue;
            }
            if (!mods.contains(Modifier.ABSTRACT)) {
                env.getMessager().printError(
                    "@LuaUnion requires the class to be `abstract` — the alias names a union of "
                    + "variants and can't be instantiated directly",
                    parentElement);
                continue;
            }

            var parent = rawExports.get(parentIdx);
            var variantTypes = new ArrayList<TypeName>();
            var variantIndices = new ArrayList<Integer>();
            boolean variantsValid = true;

            for (var permittedMirror : parentElement.getPermittedSubclasses()) {
                var permittedName = TypeName.get(permittedMirror);
                var variantIdx = byJavaType.get(permittedName);
                if (variantIdx == null) {
                    var permittedElement = ((javax.lang.model.type.DeclaredType) permittedMirror)
                        .asElement();
                    env.getMessager().printError(
                        "@LuaUnion permitted variant '" + permittedName
                        + "' is missing @LuaExport — every variant must be exported",
                        permittedElement);
                    variantsValid = false;
                    continue;
                }

                var variant = rawExports.get(variantIdx);
                if (variant.superExport() == null
                    || !variant.superExport().equals(parent.javaType())) {
                    env.getMessager().printError(
                        "@LuaUnion variant '" + variant.luaName() + "' must extend its union "
                        + "parent '" + parent.luaName() + "' directly (no intermediate class)",
                        exportElements.get(variantIdx));
                    variantsValid = false;
                    continue;
                }

                variantTypes.add(permittedName);
                variantIndices.add(variantIdx);
            }

            String discriminator = luaUnion.discriminator().isEmpty() ? null : luaUnion.discriminator();
            if (discriminator != null && variantsValid)
                validateDiscriminator(parent, discriminator, variantIndices, rawExports, exportElements);

            // Stamp variants first, then parent, so the rewrite is atomic per family.
            for (int idx : variantIndices)
                rawExports.set(idx, withKind(rawExports.get(idx),
                    Model.Export.Kind.UNION_VARIANT, List.of(), null));
            rawExports.set(parentIdx, withKind(parent,
                Model.Export.Kind.UNION_ALIAS, List.copyOf(variantTypes), discriminator));
        }
    }

    /// Per-variant discriminator contract: every variant must declare a [LuaProperty] named
    /// `discriminator` whose return type is a [LuauType.StringLiteral], and the literals must be
    /// pairwise distinct across the family. The literal must come from `@luaReturn "..."` on the
    /// getter — anything else (plain `string`, a union of literals, missing tag) is rejected.
    private void validateDiscriminator(
        Model.Export parent, String discriminator,
        List<Integer> variantIndices, List<Model.Export> rawExports, List<TypeElement> exportElements
    ) {
        var literalToVariant = new LinkedHashMap<String, String>();
        for (int idx : variantIndices) {
            var variant = rawExports.get(idx);
            var element = exportElements.get(idx);
            var prop = findProperty(variant, discriminator);
            if (prop == null || prop.getter() == null) {
                env.getMessager().printError(
                    "@LuaUnion(discriminator = \"" + discriminator + "\") requires variant '"
                    + variant.luaName() + "' to declare a @LuaProperty getter named '"
                    + discriminator + "' returning a string literal type",
                    element);
                continue;
            }
            if (!(prop.getter().type() instanceof LuauType.StringLiteral lit)) {
                env.getMessager().printError(
                    "@LuaUnion(discriminator = \"" + discriminator + "\") requires variant '"
                    + variant.luaName() + "'.'" + discriminator + "' to be typed as a string "
                    + "literal (e.g. `@luaReturn \"" + variant.luaName().toLowerCase() + "\"`)",
                    element);
                continue;
            }
            var prior = literalToVariant.put(lit.value(), variant.luaName());
            if (prior != null) {
                env.getMessager().printError(
                    "@LuaUnion(discriminator = \"" + discriminator + "\") literal \""
                    + lit.value() + "\" is shared by variants '" + prior + "' and '"
                    + variant.luaName() + "' — discriminator values must be pairwise distinct",
                    element);
            }
        }
    }

    private static @Nullable Model.Property findProperty(Model.Export ex, String luaName) {
        for (var p : ex.properties()) if (luaName.equals(p.luaName())) return p;
        return null;
    }

    private static Model.Export withKind(
        Model.Export ex, Model.Export.Kind kind, List<TypeName> unionVariants,
        @Nullable String discriminator
    ) {
        return new Model.Export(
            ex.javaType(), ex.luaName(), ex.superExport(), ex.isFinal(),
            ex.generics(), ex.properties(), ex.methods(), ex.metaMethods(),
            ex.userDataTag(), ex.hasSubtypes(),
            kind, unionVariants, discriminator, ex.description());
    }

    /// Reject method-level `@luaGeneric` declarations that reuse a name already bound by an
    /// `@luaGeneric` on the enclosing `@LuaExport` class. The type-level generic is already
    /// in scope for every member; redeclaring it on a single method would be ambiguous.
    private void checkGenericShadowing(
        ExecutableElement method, Docs docs, Set<String> typeGenericNames, String typeLuaName
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
        TypeName enclosingType, Docs docs
    ) {
        return new Model.Method(
            luaName, javaName, isVoid, enclosingType,
            docs.description(),
            mapGenerics(docs.generics()),
            mapParams(source, docs.params()),
            mapReturns(source, docs.returns()));
    }

    private Model.MetaMethod buildMetaMethod(
        ExecutableElement source, String meta, String javaName, boolean isVoid, Docs docs
    ) {
        return new Model.MetaMethod(
            meta, javaName, isVoid,
            docs.description(),
            mapGenerics(docs.generics()),
            mapParams(source, docs.params()),
            mapReturns(source, docs.returns()));
    }

    private Model.Accessor buildGetter(
        ExecutableElement source, String javaName, TypeName enclosingType, Docs docs
    ) {
        var firstReturn = docs.returns().isEmpty() ? null : docs.returns().get(0);
        LuauType type = parseTypeOrNil(source, firstReturn == null ? "" : firstReturn.typeExpr());
        return new Model.Accessor(javaName, enclosingType, docs.description(), null, type);
    }

    private Model.Accessor buildSetter(
        ExecutableElement source, String javaName, TypeName enclosingType, Docs docs
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

    private List<Model.GenericParam> mapGenerics(List<DocTag.Generic> generics) {
        var out = new ArrayList<Model.GenericParam>(generics.size());
        for (var g : generics) out.add(new Model.GenericParam(g.name(), g.pack(), g.description()));
        return out;
    }

    private List<Model.Param> mapParams(ExecutableElement source, List<DocTag.Param> params) {
        var out = new ArrayList<Model.Param>(params.size());
        for (var p : params)
            out.add(new Model.Param(
                p.name(), p.optional(), parseTypeOrNil(source, p.typeExpr()), p.description()));
        return out;
    }

    private List<Model.Return> mapReturns(ExecutableElement source, List<DocTag.Return> returns) {
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

    private Docs parseDocs(Element element) {
        return JavadocTagParser.parse(env.getElementUtils().getDocComment(element));
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
