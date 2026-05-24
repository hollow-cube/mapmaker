package net.hollowcube.scripting;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.hollowcube.scripting.types.LuauType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/// Write-only JSON emitter for the engine API. Produces the published `engine-api.json` document
/// from an in-memory [Schema]. There is no read side — the JSON is a build artifact for the
/// editor / external consumers, and the AP holds the authoritative model in memory.
///
/// The published shape is deliberately slimmer than the in-memory [Model]: it carries no Java
/// implementation detail. Concretely, relative to the IR:
///
///  - `@LuaLibrary` scope is not stored — require-d libraries live under the `libraries` object,
///    global libraries under the `globals` array, so the location already says it.
///  - the Lua-facing name is published as `name` (the IR calls it `luaName`).
///  - `javaMethodName`, `enclosingType`, and `isVoid` are dropped (a method with no `returns`
///    is the void case).
///
/// Type expressions are fully-resolved [LuauType] AST. Sealed-type discriminators use a `kind`
/// field on the JSON object.
public final class SchemaJson {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String KIND = "luau-engine-api";

    private static final Gson GSON = new GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .disableJdkUnsafe()
        .registerTypeAdapter(ClassName.class, new ClassNameAdapter())
        .registerTypeAdapter(TypeName.class, new TypeNameAdapter())
        .registerTypeAdapterFactory(new ModelAdapterFactory())
        .registerTypeAdapterFactory(new LuauTypeAdapterFactory())
        .registerTypeAdapterFactory(new TypeArgAdapterFactory())
        .create();

    private SchemaJson() {
    }

    public static String toJson(Schema schema) {
        return GSON.toJson(schema);
    }

    // =========================================================================
    // Adapters — write-only. read() throws to keep the TypeAdapter contract.
    // =========================================================================

    private static abstract class WriteOnly<T> extends TypeAdapter<T> {
        @Override
        public final T read(JsonReader in) {
            throw new UnsupportedOperationException("SchemaJson is write-only");
        }
    }

    private static final class ClassNameAdapter extends WriteOnly<ClassName> {
        @Override
        public void write(JsonWriter out, @Nullable ClassName value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.canonicalName());
        }
    }

    private static final class TypeNameAdapter extends WriteOnly<TypeName> {
        @Override
        public void write(JsonWriter out, @Nullable TypeName value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toString());
        }
    }

    // =========================================================================
    // Schema / Model.* — slim published shape (see class doc)
    // =========================================================================

    private static final class ModelAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings("unchecked")
        @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Class<?> raw = typeToken.getRawType();
            if (raw == Schema.class) return (TypeAdapter<T>) new SchemaAdapter(gson).nullSafe();
            if (raw == Model.Library.class) return (TypeAdapter<T>) new LibraryAdapter(gson).nullSafe();
            if (raw == Model.Export.class) return (TypeAdapter<T>) new ExportAdapter(gson).nullSafe();
            if (raw == Model.Property.class) return (TypeAdapter<T>) new PropertyAdapter(gson).nullSafe();
            if (raw == Model.Accessor.class) return (TypeAdapter<T>) new AccessorAdapter(gson).nullSafe();
            if (raw == Model.Method.class) return (TypeAdapter<T>) new MethodAdapter(gson).nullSafe();
            if (raw == Model.MetaMethod.class) return (TypeAdapter<T>) new MetaMethodAdapter(gson).nullSafe();
            return null;
        }
    }

    private static final Type EXPORT_LIST = new TypeToken<List<Model.Export>>() {}.getType();
    private static final Type METHOD_LIST = new TypeToken<List<Model.Method>>() {}.getType();
    private static final Type META_LIST = new TypeToken<List<Model.MetaMethod>>() {}.getType();
    private static final Type PROPERTY_LIST = new TypeToken<List<Model.Property>>() {}.getType();
    private static final Type GENERIC_LIST = new TypeToken<List<Model.GenericParam>>() {}.getType();
    private static final Type PARAM_LIST = new TypeToken<List<Model.Param>>() {}.getType();
    private static final Type RETURN_LIST = new TypeToken<List<Model.Return>>() {}.getType();
    private static final Type TYPE_NAME_LIST = new TypeToken<List<TypeName>>() {}.getType();
    private static final Type LUAU_TYPE_LIST = new TypeToken<List<LuauType>>() {}.getType();
    private static final Type TYPE_ARG_LIST = new TypeToken<List<LuauType.TypeArg>>() {}.getType();

    /// Splits libraries by `@LuaLibrary` scope so the on-disk shape encodes scope structurally.
    private static final class SchemaAdapter extends WriteOnly<Schema> {
        private final Gson gson;

        SchemaAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Schema value) throws IOException {
            var libraries = new JsonObject();
            var globals = new JsonArray();
            for (var lib : value.libraries().values()) {
                var libJson = gson.toJsonTree(lib, Model.Library.class);
                if (lib.scope() == LuaLibrary.Scope.GLOBAL) globals.add(libJson);
                else libraries.add(lib.moduleName(), libJson);
            }
            var o = new JsonObject();
            o.addProperty("schemaVersion", value.schemaVersion());
            o.addProperty("kind", value.kind());
            o.add("libraries", libraries);
            o.add("globals", globals);
            gson.toJson(o, out);
        }
    }

    private static final class LibraryAdapter extends WriteOnly<Model.Library> {
        private final Gson gson;

        LibraryAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Model.Library v) throws IOException {
            var o = new JsonObject();
            o.add("sourceType", gson.toJsonTree(v.sourceType(), ClassName.class));
            o.add("glueType", gson.toJsonTree(v.glueType(), ClassName.class));
            o.addProperty("moduleName", v.moduleName());
            o.add("exports", gson.toJsonTree(v.exports(), EXPORT_LIST));
            o.add("staticMethods", gson.toJsonTree(v.staticMethods(), METHOD_LIST));
            o.add("staticProperties", gson.toJsonTree(v.staticProperties(), PROPERTY_LIST));
            o.addProperty("description", v.description());
            gson.toJson(o, out);
        }
    }

    private static final class ExportAdapter extends WriteOnly<Model.Export> {
        private final Gson gson;

        ExportAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Model.Export v) throws IOException {
            var o = new JsonObject();
            o.add("javaType", gson.toJsonTree(v.javaType(), TypeName.class));
            o.addProperty("name", v.luaName());
            o.add("superExport", gson.toJsonTree(v.superExport(), TypeName.class));
            o.addProperty("isFinal", v.isFinal());
            o.add("generics", gson.toJsonTree(v.generics(), GENERIC_LIST));
            o.add("properties", gson.toJsonTree(v.properties(), PROPERTY_LIST));
            o.add("methods", gson.toJsonTree(v.methods(), METHOD_LIST));
            o.add("metaMethods", gson.toJsonTree(v.metaMethods(), META_LIST));
            o.addProperty("userDataTag", v.userDataTag());
            o.addProperty("hasSubtypes", v.hasSubtypes());
            if (v.kind() != Model.Export.Kind.STRUCT)
                o.addProperty("unionKind", v.kind().name());
            if (!v.unionVariants().isEmpty())
                o.add("unionVariants", gson.toJsonTree(v.unionVariants(), TYPE_NAME_LIST));
            if (v.discriminator() != null)
                o.addProperty("discriminator", v.discriminator());
            o.addProperty("description", v.description());
            gson.toJson(o, out);
        }
    }

    private static final class PropertyAdapter extends WriteOnly<Model.Property> {
        private final Gson gson;

        PropertyAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Model.Property v) throws IOException {
            var o = new JsonObject();
            o.addProperty("name", v.luaName());
            o.add("getter", gson.toJsonTree(v.getter(), Model.Accessor.class));
            o.add("setter", gson.toJsonTree(v.setter(), Model.Accessor.class));
            gson.toJson(o, out);
        }
    }

    private static final class AccessorAdapter extends WriteOnly<Model.Accessor> {
        private final Gson gson;

        AccessorAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Model.Accessor v) throws IOException {
            var o = new JsonObject();
            o.addProperty("description", v.description());
            o.addProperty("paramName", v.paramName());
            o.add("type", gson.toJsonTree(v.type(), LuauType.class));
            gson.toJson(o, out);
        }
    }

    private static final class MethodAdapter extends WriteOnly<Model.Method> {
        private final Gson gson;

        MethodAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Model.Method v) throws IOException {
            var o = new JsonObject();
            o.addProperty("name", v.luaName());
            o.addProperty("description", v.description());
            o.add("generics", gson.toJsonTree(v.generics(), GENERIC_LIST));
            o.add("params", gson.toJsonTree(v.params(), PARAM_LIST));
            o.add("returns", gson.toJsonTree(v.returns(), RETURN_LIST));
            gson.toJson(o, out);
        }
    }

    private static final class MetaMethodAdapter extends WriteOnly<Model.MetaMethod> {
        private final Gson gson;

        MetaMethodAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Model.MetaMethod v) throws IOException {
            var o = new JsonObject();
            o.addProperty("meta", v.meta());
            o.addProperty("description", v.description());
            o.add("generics", gson.toJsonTree(v.generics(), GENERIC_LIST));
            o.add("params", gson.toJsonTree(v.params(), PARAM_LIST));
            o.add("returns", gson.toJsonTree(v.returns(), RETURN_LIST));
            gson.toJson(o, out);
        }
    }

    // =========================================================================
    // LuauType (sealed) — discriminated by "kind"
    // =========================================================================

    private static final class LuauTypeAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings("unchecked")
        @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            if (!LuauType.class.isAssignableFrom(typeToken.getRawType())) return null;
            return (TypeAdapter<T>) new LuauTypeAdapter(gson).nullSafe();
        }
    }

    private static final class LuauTypeAdapter extends WriteOnly<LuauType> {
        private final Gson gson;

        LuauTypeAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, LuauType value) throws IOException {
            JsonObject o = new JsonObject();
            switch (value) {
                case LuauType.Named n -> {
                    o.addProperty("kind", "named");
                    o.addProperty("module", n.module());
                    o.addProperty("name", n.name());
                    o.add("args", gson.toJsonTree(n.args(), TYPE_ARG_LIST));
                }
                case LuauType.GenericRef g -> {
                    o.addProperty("kind", "genericRef");
                    o.addProperty("name", g.name());
                }
                case LuauType.Optional opt -> {
                    o.addProperty("kind", "optional");
                    o.add("inner", gson.toJsonTree(opt.inner(), LuauType.class));
                }
                case LuauType.Union u -> {
                    o.addProperty("kind", "union");
                    o.add("alternatives", gson.toJsonTree(u.alternatives(), LUAU_TYPE_LIST));
                }
                case LuauType.Intersection it -> {
                    o.addProperty("kind", "intersection");
                    o.add("conjuncts", gson.toJsonTree(it.conjuncts(), LUAU_TYPE_LIST));
                }
                case LuauType.Table t -> {
                    o.addProperty("kind", "table");
                    JsonArray fields = new JsonArray();
                    for (var f : t.fields()) {
                        JsonObject fo = new JsonObject();
                        fo.addProperty("name", f.name());
                        fo.add("type", gson.toJsonTree(f.type(), LuauType.class));
                        fields.add(fo);
                    }
                    o.add("fields", fields);
                    o.add("arrayElement", t.arrayElement() == null ? null : gson.toJsonTree(t.arrayElement(), LuauType.class));
                    o.add("indexerKey", t.indexerKey() == null ? null : gson.toJsonTree(t.indexerKey(), LuauType.class));
                    o.add("indexerValue", t.indexerValue() == null ? null : gson.toJsonTree(t.indexerValue(), LuauType.class));
                }
                case LuauType.Function f -> {
                    o.addProperty("kind", "function");
                    JsonArray params = new JsonArray();
                    for (var p : f.params()) {
                        JsonObject po = new JsonObject();
                        po.addProperty("name", p.name());
                        po.add("type", gson.toJsonTree(p.type(), LuauType.class));
                        params.add(po);
                    }
                    o.add("params", params);
                    o.add("varargs", f.varargs() == null ? null : gson.toJsonTree(f.varargs(), LuauType.Variadic.class));
                    o.add("returns", gson.toJsonTree(f.returns(), LUAU_TYPE_LIST));
                }
                case LuauType.Variadic v -> {
                    o.addProperty("kind", "variadic");
                    o.add("element", gson.toJsonTree(v.element(), LuauType.class));
                }
                case LuauType.GenericPack gp -> {
                    o.addProperty("kind", "genericPack");
                    o.addProperty("name", gp.name());
                }
                case LuauType.TypeOf to -> {
                    o.addProperty("kind", "typeof");
                    o.addProperty("expr", to.expr());
                }
                case LuauType.StringLiteral sl -> {
                    o.addProperty("kind", "string");
                    o.addProperty("value", sl.value());
                }
                case LuauType.BoolLiteral bl -> {
                    o.addProperty("kind", "bool");
                    o.addProperty("value", bl.value());
                }
            }
            gson.toJson(o, out);
        }
    }

    // =========================================================================
    // LuauType.TypeArg (sealed) — discriminated by "kind"
    // =========================================================================

    private static final class TypeArgAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings("unchecked")
        @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            if (!LuauType.TypeArg.class.isAssignableFrom(typeToken.getRawType())) return null;
            return (TypeAdapter<T>) new TypeArgAdapter(gson).nullSafe();
        }
    }

    private static final class TypeArgAdapter extends WriteOnly<LuauType.TypeArg> {
        private final Gson gson;

        TypeArgAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, LuauType.TypeArg value) throws IOException {
            JsonObject o = new JsonObject();
            switch (value) {
                case LuauType.TypeArg.Single s -> {
                    o.addProperty("kind", "single");
                    o.add("type", gson.toJsonTree(s.type(), LuauType.class));
                }
                case LuauType.TypeArg.Pack p -> {
                    o.addProperty("kind", "pack");
                    o.add("types", gson.toJsonTree(p.types(), LUAU_TYPE_LIST));
                    o.add("tail", p.tail() == null ? null : gson.toJsonTree(p.tail(), LuauType.Variadic.class));
                }
            }
            gson.toJson(o, out);
        }
    }
}
