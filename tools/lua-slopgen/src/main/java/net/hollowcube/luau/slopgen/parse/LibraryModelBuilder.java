package net.hollowcube.luau.slopgen.parse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.gen.*;
import net.hollowcube.luau.slopgen.LuaNames;
import net.hollowcube.luau.slopgen.docs.JavadocTagParser;
import net.hollowcube.luau.slopgen.docs.LuaDocsValidator;
import net.hollowcube.luau.slopgen.docs.MemberDocs;
import net.hollowcube.luau.slopgen.model.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.*;

/// Walks a `@LuaLibrary`-annotated class and produces an immutable [LibrarySpec]. Atom and
/// userdata-tag assignments are accumulated into shared tables provided by the caller — those are
/// later consumed by the atom-table emitter.
///
/// Diagnostics for malformed inputs are reported via the processor `Messager`. A future validator
/// pass (see plan) will move these out of this class.
public final class LibraryModelBuilder {

    private final ProcessingEnvironment env;
    private final AtomTable atomTable;
    private final UserDataTagTable userDataTagTable;
    private final LuaDocsValidator docsValidator;

    public LibraryModelBuilder(ProcessingEnvironment env, AtomTable atomTable, UserDataTagTable userDataTagTable) {
        this.env = env;
        this.atomTable = atomTable;
        this.userDataTagTable = userDataTagTable;
        this.docsValidator = new LuaDocsValidator(env.getMessager());
    }

    public @Nullable LibrarySpec build(TypeElement libraryClass) {
        var luaLibrary = libraryClass.getAnnotation(LuaLibrary.class);
        if (luaLibrary == null) return null;

        var packageName = env.getElementUtils().getPackageOf(libraryClass).getQualifiedName().toString();
        var sourceType = ClassName.get(packageName, libraryClass.getSimpleName().toString());
        var glueType = ClassName.get(packageName, libraryClass.getSimpleName() + "$luau");

        var libraryDocs = parseDocs(libraryClass);
        docsValidator.validateContainer(libraryClass, libraryDocs, "library");

        var staticGetters = new LinkedHashMap<String, AccessorSpec>();
        var staticSetters = new LinkedHashMap<String, AccessorSpec>();
        var staticMethods = new ArrayList<MethodSpec>();
        var rawExports = new ArrayList<ExportSpec>();

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
                var accessor = new AccessorSpec(javaName, enclosingType, memberDocs);
                if (javaName.startsWith("set")) {
                    docsValidator.validateSetter(method, memberDocs);
                    staticSetters.put(luaName, accessor);
                } else {
                    docsValidator.validateGetter(method, memberDocs);
                    staticGetters.put(luaName, accessor);
                }
            } else {
                var luaName = LuaNames.toLuaMethod(javaName);
                var isVoid = method.getReturnType().getKind() == TypeKind.VOID;
                docsValidator.validateMethod(method, memberDocs, isVoid);
                staticMethods.add(new MethodSpec(luaName, javaName, isVoid, enclosingType, memberDocs));
            }
        }

        var staticProperties = mergeAccessors(staticGetters, staticSetters);
        validateAccessorPairing(staticProperties, libraryClass);

        // Reserve atoms in source-declaration order so the emitter's literal references match.
        for (var p : staticProperties) atomTable.atomFor(p.luaName());
        for (var m : staticMethods) atomTable.atomFor(m.luaName());

        var supersUsed = new HashSet<TypeName>();
        for (var ex : rawExports)
            if (ex.superExport() != null) supersUsed.add(ex.superExport());

        var finalizedExports = new ArrayList<ExportSpec>(rawExports.size());
        for (var ex : rawExports)
            finalizedExports.add(ex.withHasSubtypes(supersUsed.contains(ex.javaType())));

        return new LibrarySpec(
            sourceType, glueType, luaLibrary.name(), luaLibrary.scope(),
            finalizedExports, staticMethods, staticProperties, libraryDocs);
    }

    private ExportSpec buildExport(TypeElement exportClass) {
        var javaType = TypeName.get(exportClass.asType());
        var luaName = exportClass.getSimpleName().toString();
        boolean isFinal = exportClass.getModifiers().contains(Modifier.FINAL);

        var exportDocs = parseDocs(exportClass);
        docsValidator.validateContainer(exportClass, exportDocs, "@LuaExport class");

        TypeName superExport = null;
        var superMirror = exportClass.getSuperclass();
        if (superMirror.getKind() != TypeKind.NONE) {
            var superTypeName = TypeName.get(superMirror);
            if (!superTypeName.equals(TypeName.get(Object.class))
                && !superTypeName.equals(TypeName.get(Record.class)))
                superExport = superTypeName;
        }

        int tag = userDataTagTable.allocate();

        var getters = new LinkedHashMap<String, AccessorSpec>();
        var setters = new LinkedHashMap<String, AccessorSpec>();
        var methods = new ArrayList<MethodSpec>();
        var metaMethods = new ArrayList<MetaSpec>();

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

            if (luaProperty != null) {
                var name = LuaNames.toLuaProperty(javaName);
                var accessor = new AccessorSpec(javaName, javaType, memberDocs);
                if (javaName.startsWith("set")) {
                    docsValidator.validateSetter(method, memberDocs);
                    setters.put(name, accessor);
                } else {
                    docsValidator.validateGetter(method, memberDocs);
                    getters.put(name, accessor);
                }
            } else if (luaMethod.meta() != Meta.NONE) {
                docsValidator.validateMeta(method, memberDocs, isVoid);
                metaMethods.add(new MetaSpec(luaMethod.meta(), javaName, isVoid, memberDocs));
            } else {
                docsValidator.validateMethod(method, memberDocs, isVoid);
                var name = LuaNames.toLuaMethod(javaName);
                methods.add(new MethodSpec(name, javaName, isVoid, javaType, memberDocs));
            }
        }

        var properties = mergeAccessors(getters, setters);
        validateAccessorPairing(properties, exportClass);

        for (var p : properties) atomTable.atomFor(p.luaName());
        for (var m : methods) atomTable.atomFor(m.luaName());

        return new ExportSpec(javaType, luaName, superExport, isFinal,
            properties, methods, metaMethods, tag, /*hasSubtypes=*/false, exportDocs);
    }

    private MemberDocs parseDocs(javax.lang.model.element.Element element) {
        return JavadocTagParser.parse(env.getElementUtils().getDocComment(element));
    }

    /// Cross-check getter/setter type expressions on a merged property list. Reports against
    /// the setter element so authors see the error at the side that's typically the reactive
    /// edit (you usually add a setter to an existing typed getter).
    private void validateAccessorPairing(List<PropertySpec> properties, javax.lang.model.element.Element fallback) {
        for (var p : properties) {
            if (p.getter() == null || p.setter() == null) continue;
            var getterDocs = p.getter().docs();
            var setterDocs = p.setter().docs();
            // The validator wants the actual setter element for IDE pinning. We don't have it
            // directly here, so re-locate via the enclosing element. In practice both accessors
            // belong to the same enclosing class, so attaching to `fallback` is acceptable until
            // we plumb the original elements through; the message text identifies the property.
            docsValidator.validatePropertyConsistency(fallback, getterDocs, setterDocs);
        }
    }

    private static List<PropertySpec> mergeAccessors(
        LinkedHashMap<String, AccessorSpec> getters,
        LinkedHashMap<String, AccessorSpec> setters
    ) {
        var seen = new LinkedHashSet<String>();
        seen.addAll(getters.keySet());
        seen.addAll(setters.keySet());
        var out = new ArrayList<PropertySpec>(seen.size());
        for (var name : seen)
            out.add(new PropertySpec(name, getters.get(name), setters.get(name)));
        return out;
    }
}
