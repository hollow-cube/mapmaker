package net.hollowcube.luau.slopgen.parse;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.gen.*;
import net.hollowcube.luau.slopgen.LuaNames;
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

    public LibraryModelBuilder(ProcessingEnvironment env, AtomTable atomTable, UserDataTagTable userDataTagTable) {
        this.env = env;
        this.atomTable = atomTable;
        this.userDataTagTable = userDataTagTable;
    }

    public @Nullable LibrarySpec build(TypeElement libraryClass) {
        var luaLibrary = libraryClass.getAnnotation(LuaLibrary.class);
        if (luaLibrary == null) return null;

        var packageName = env.getElementUtils().getPackageOf(libraryClass).getQualifiedName().toString();
        var sourceType = ClassName.get(packageName, libraryClass.getSimpleName().toString());
        var glueType = ClassName.get(packageName, libraryClass.getSimpleName() + "$luau");

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

            if (luaProperty != null) {
                var luaName = LuaNames.toLuaProperty(javaName);
                var accessor = new AccessorSpec(javaName, enclosingType);
                if (javaName.startsWith("set"))
                    staticSetters.put(luaName, accessor);
                else
                    staticGetters.put(luaName, accessor);
            } else {
                var luaName = LuaNames.toLuaMethod(javaName);
                var isVoid = method.getReturnType().getKind() == TypeKind.VOID;
                staticMethods.add(new MethodSpec(luaName, javaName, isVoid, enclosingType));
            }
        }

        var staticProperties = mergeAccessors(staticGetters, staticSetters);

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
            finalizedExports, staticMethods, staticProperties);
    }

    private ExportSpec buildExport(TypeElement exportClass) {
        var javaType = TypeName.get(exportClass.asType());
        var luaName = exportClass.getSimpleName().toString();
        boolean isFinal = exportClass.getModifiers().contains(Modifier.FINAL);

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

            if (luaProperty != null) {
                var name = LuaNames.toLuaProperty(javaName);
                var accessor = new AccessorSpec(javaName, javaType);
                if (javaName.startsWith("set"))
                    setters.put(name, accessor);
                else
                    getters.put(name, accessor);
            } else if (luaMethod.meta() != Meta.NONE) {
                metaMethods.add(new MetaSpec(luaMethod.meta(), javaName, isVoid));
            } else {
                var name = LuaNames.toLuaMethod(javaName);
                methods.add(new MethodSpec(name, javaName, isVoid, javaType));
            }
        }

        var properties = mergeAccessors(getters, setters);

        for (var p : properties) atomTable.atomFor(p.luaName());
        for (var m : methods) atomTable.atomFor(m.luaName());

        return new ExportSpec(javaType, luaName, superExport, isFinal,
            properties, methods, metaMethods, tag, /*hasSubtypes=*/false);
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
