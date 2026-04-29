package net.hollowcube.luau.slopgen.model;

import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.docs.MemberDocs;

import java.util.List;

/// A single `@LuaLibrary` translated into the IR. `glueType` is the generated companion class
/// (typically `<sourceType.simpleName()>$luau`). `staticMethods` and `staticProperties` are the
/// top-level (library-level) `@LuaMethod` / `@LuaProperty` declarations; per-export members live
/// on each [ExportSpec].
public record LibrarySpec(
    ClassName sourceType,
    ClassName glueType,
    String moduleName,
    LuaLibrary.Scope scope,
    List<ExportSpec> exports,
    List<MethodSpec> staticMethods,
    List<PropertySpec> staticProperties,
    MemberDocs docs
) {}
