package net.hollowcube.luau.slopgen.model;

import com.palantir.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/// One `@LuaExport` inside a library. Inheritance between exports is captured by `superExport`
/// (a [TypeName] pointing at another `ExportSpec.javaType` within the same library, or null when
/// the export's nearest exported ancestor is `Object` / `Record` / a non-exported type).
///
/// `hasSubtypes` is populated by the parse pass after all exports are known. `isFinal` reflects
/// the source-level Java modifier; the emitter uses (`!isFinal || superExport != null`) to decide
/// whether to take the dispatch path.
public record ExportSpec(
    TypeName javaType,
    String luaName,
    @Nullable TypeName superExport,
    boolean isFinal,
    List<PropertySpec> properties,
    List<MethodSpec> methods,
    List<MetaSpec> metaMethods,
    int userDataTag,
    boolean hasSubtypes
) {
    public ExportSpec withHasSubtypes(boolean hasSubtypes) {
        return new ExportSpec(javaType, luaName, superExport, isFinal,
            properties, methods, metaMethods, userDataTag, hasSubtypes);
    }
}
