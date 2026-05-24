package net.hollowcube.luau.slopgen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.types.LuauType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/// Internal IR for a `@LuaLibrary`. All Luau type expressions on methods, accessors, params,
/// and returns are stored as fully-resolved [LuauType] AST — string parsing happens once during
/// model build, never on the consumer side.
///
/// This is also the shape published to JSON. Per-library fragments and the aggregated engine
/// API are both [Schema] documents wrapping a map of `Library` entries; see [SchemaJson].
public sealed interface Model {

    record Library(
        ClassName sourceType,
        ClassName glueType,
        String moduleName,
        LuaLibrary.Scope scope,
        List<Export> exports,
        List<Method> staticMethods,
        List<Property> staticProperties,
        String description
    ) implements Model {
    }

    record Export(
        TypeName javaType,
        String luaName,
        @Nullable TypeName superExport,
        boolean isFinal,
        List<GenericParam> generics,
        List<Property> properties,
        List<Method> methods,
        List<MetaMethod> metaMethods,
        int userDataTag,
        boolean hasSubtypes,
        Kind kind,
        List<TypeName> unionVariants,
        @Nullable String discriminator,
        String description
    ) implements Model {
        /// Classifies an export for type emission:
        ///
        ///  - [Kind#STRUCT]: a normal `@LuaExport` — emits as `export type Name = (Super &)? {...}`.
        ///  - [Kind#UNION_ALIAS]: an `@LuaExport @LuaUnion` abstract parent — its declared members
        ///    emit as a synthetic non-exported common shape (`__NameMembers`), and the public
        ///    `export type Name = V1 | V2` alias names the union of its variants.
        ///  - [Kind#UNION_VARIANT]: a permitted subtype of a [Kind#UNION_ALIAS] parent — emits as
        ///    `export type Name = __ParentMembers & {...}`, anchored on the synthetic shape
        ///    instead of the union alias itself (avoids a circular reference).
        public enum Kind { STRUCT, UNION_ALIAS, UNION_VARIANT }

        /// Back-compat constructor: pre-union call sites (tests, fixtures, JSON read-back without
        /// `unionKind`) keep their argument shape and get a [Kind#STRUCT] export by default.
        public Export(
            TypeName javaType,
            String luaName,
            @Nullable TypeName superExport,
            boolean isFinal,
            List<GenericParam> generics,
            List<Property> properties,
            List<Method> methods,
            List<MetaMethod> metaMethods,
            int userDataTag,
            boolean hasSubtypes,
            String description
        ) {
            this(javaType, luaName, superExport, isFinal, generics, properties, methods,
                metaMethods, userDataTag, hasSubtypes, Kind.STRUCT, List.of(), null, description);
        }

        public Export withSubtypes(boolean hasSubtypes) {
            return new Export(javaType, luaName, superExport, isFinal,
                generics, properties, methods, metaMethods, userDataTag, hasSubtypes,
                kind, unionVariants, discriminator, description);
        }
    }

    record Method(
        String luaName,
        String javaMethodName,
        boolean isVoid,
        TypeName enclosingType,
        String description,
        List<GenericParam> generics,
        List<Param> params,
        List<Return> returns
    ) implements Model {
    }

    record MetaMethod(
        String meta,
        String javaMethodName,
        boolean isVoid,
        String description,
        List<GenericParam> generics,
        List<Param> params,
        List<Return> returns
    ) implements Model {
    }

    record Property(
        String luaName,
        @Nullable Accessor getter,
        @Nullable Accessor setter
    ) implements Model {
        public Property {
            if (getter == null && setter == null)
                throw new IllegalArgumentException("Property '" + luaName + "' has neither getter nor setter");
        }
    }

    /// Getter or setter accessor on a property. For getters, `paramName` is null and `type` is
    /// the return type. For setters, `paramName` is the source `@luaParam` name and `type` is
    /// that param's type.
    record Accessor(
        String javaMethodName,
        TypeName enclosingType,
        String description,
        @Nullable String paramName,
        LuauType type
    ) implements Model {
    }

    record Param(String name, boolean optional, LuauType type, String description) {}

    record Return(LuauType type, String description) {}

    record GenericParam(String name, boolean pack, String description) {}

}
