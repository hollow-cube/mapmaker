package net.hollowcube.luau.slopgen;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.types.LuauType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/// Single source of truth for translating [Schema] / [Model] / [LuauType] documents to and from
/// JSON. The emitted shape is the canonical published form: per-library fragments use the same
/// schema as the aggregated engine API, just with one entry in `libraries` (or `globals`).
///
/// The published shape is deliberately slimmer than the in-memory [Model]: it carries no Java
/// implementation detail (a `.d.luau`/docs consumer doesn't need it and the file is large).
/// Concretely, relative to the IR:
///
///  - `@LuaLibrary` scope is not stored — require-d libraries live under the `libraries` object,
///    global libraries under the `globals` array, so the location already says it.
///  - the Lua-facing name is published as `name` (the IR calls it `luaName`).
///  - `javaMethodName`, `enclosingType`, and `isVoid` are dropped (a method with no `returns`
///    is the void case; codegen reads these from the in-memory model, never from JSON).
///
/// Read-back reconstructs the IR with placeholders for the dropped Java fields — nothing that
/// consumes the JSON (cross-module resolution, diffing, declaration emit) depends on them.
///
/// Type expressions inside library bodies are fully-resolved [LuauType] AST — string parsing
/// happens once in the annotation processor.
///
/// Sealed-type discriminators use a `kind` field on the JSON object.
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

    /// Minified writer for the editor variant (see [#toEditorJson]). No pretty-printing; nulls
    /// are already pruned from the tree so `serializeNulls` is irrelevant here.
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    /// Keys carrying Java/codegen implementation detail. Stripped from the editor variant — the
    /// editor only needs the Luau type surface. None collide with [LuauType] object keys.
    private static final java.util.Set<String> JAVA_ONLY_KEYS = java.util.Set.of(
        "sourceType", "glueType", "javaType", "isFinal", "userDataTag", "hasSubtypes");

    private SchemaJson() {
    }

    /// Build a single-library [Schema] fragment. The aggregator merges fragments by `moduleName`.
    public static Schema fragment(Model.Library library) {
        var libs = new LinkedHashMap<String, Model.Library>();
        libs.put(library.moduleName(), library);
        return new Schema(CURRENT_SCHEMA_VERSION, KIND, libs);
    }

    public static String toJson(Schema schema) {
        return GSON.toJson(schema);
    }

    public static void writeFragment(Model.Library library, Writer writer) {
        GSON.toJson(fragment(library), writer);
    }

    public static Schema read(Reader reader) {
        return GSON.fromJson(reader, Schema.class);
    }

    public static Schema read(String json) {
        return GSON.fromJson(json, Schema.class);
    }

    /// The editor variant: the same Luau API surface as [#toJson], but stripped of every Java /
    /// codegen detail and emitted as small as possible. This is the form shipped to the editor
    /// app for type info, so it is write-only (never read back into the pipeline) and optimized
    /// purely for size:
    ///
    ///  - `sourceType` / `glueType` / `javaType` / `isFinal` / `userDataTag` / `hasSubtypes` are
    ///    dropped — none describe the Luau API.
    ///  - `superExport` is published as the parent export's Luau name (the simple name) rather
    ///    than a Java type; the inheritance edge itself is Luau-relevant.
    ///  - `null`s and empty arrays/objects/strings are pruned (absent == default), and the JSON
    ///    is minified.
    public static String toEditorJson(Schema schema) {
        var tree = GSON.toJsonTree(schema, Schema.class);
        slim(tree);
        return COMPACT.toJson(tree);
    }

    /// Recursively rewrite a full-form tree into the editor shape: drop Java-only keys, fold
    /// `superExport` to a simple name, then prune anything empty. Bottom-up so a child that
    /// becomes empty is pruned by its parent.
    private static void slim(JsonElement el) {
        if (el instanceof JsonArray arr) {
            for (var child : arr) slim(child);
            return;
        }
        if (!(el instanceof JsonObject o)) return;

        for (var key : JAVA_ONLY_KEYS) o.remove(key);

        if (o.has("superExport")) {
            var sup = o.get("superExport");
            o.remove("superExport");
            if (sup.isJsonPrimitive()) {
                var fqcn = sup.getAsString();
                o.addProperty("superExport", fqcn.substring(fqcn.lastIndexOf('.') + 1));
            }
        }

        for (var entry : new ArrayList<>(o.entrySet())) {
            slim(entry.getValue());
            if (isEmpty(entry.getValue())) o.remove(entry.getKey());
        }
    }

    private static boolean isEmpty(JsonElement el) {
        if (el.isJsonNull()) return true;
        if (el instanceof JsonArray a) return a.isEmpty();
        if (el instanceof JsonObject ob) return ob.isEmpty();
        return el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()
               && el.getAsString().isEmpty();
    }

    /// `ClassName` ↔ canonical-name string. Round-trips through `ClassName.bestGuess`, which
    /// matches the names emitted by `TypeName.toString()` for non-parameterized references.
    private static final class ClassNameAdapter extends TypeAdapter<ClassName> {
        @Override
        public void write(JsonWriter out, @Nullable ClassName value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.canonicalName());
        }

        @Override
        public @Nullable ClassName read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return ClassName.bestGuess(in.nextString());
        }
    }

    /// `TypeName` ↔ canonical-name string. We only ever serialize class-type TypeNames in this
    /// schema (no parameterized exports today), so deserialization lifts back to `ClassName`.
    private static final class TypeNameAdapter extends TypeAdapter<TypeName> {
        @Override
        public void write(JsonWriter out, @Nullable TypeName value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toString());
        }

        @Override
        public @Nullable TypeName read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return ClassName.bestGuess(in.nextString());
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

    /// `{schemaVersion, kind, libraries:{moduleName→Library}, globals:[Library]}`. The split by
    /// `@LuaLibrary` scope replaces a per-library `scope` field; read-back restores the scope
    /// from which collection a library came from.
    private static final class SchemaAdapter extends TypeAdapter<Schema> {
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

        @Override
        public Schema read(JsonReader in) throws IOException {
            JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
            int schemaVersion = o.get("schemaVersion").getAsInt();
            String kind = o.get("kind").getAsString();
            var byModule = new LinkedHashMap<String, Model.Library>();
            var libraries = o.getAsJsonObject("libraries");
            if (libraries != null) {
                for (var e : libraries.entrySet()) {
                    var lib = withScope(gson.fromJson(e.getValue(), Model.Library.class),
                        LuaLibrary.Scope.REQUIRE);
                    byModule.put(lib.moduleName(), lib);
                }
            }
            var globals = o.getAsJsonArray("globals");
            if (globals != null) {
                for (var el : globals) {
                    var lib = withScope(gson.fromJson(el, Model.Library.class),
                        LuaLibrary.Scope.GLOBAL);
                    byModule.put(lib.moduleName(), lib);
                }
            }
            return new Schema(schemaVersion, kind, byModule);
        }

        private static Model.Library withScope(Model.Library l, LuaLibrary.Scope scope) {
            return new Model.Library(l.sourceType(), l.glueType(), l.moduleName(), scope,
                l.exports(), l.staticMethods(), l.staticProperties(), l.description());
        }
    }

    /// Library without the `scope` field — implied by the enclosing `libraries`/`globals`
    /// collection. Read-back leaves `scope` at `REQUIRE`; [SchemaAdapter] corrects it.
    private static final class LibraryAdapter extends TypeAdapter<Model.Library> {
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

        @Override
        public Model.Library read(JsonReader in) {
            JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
            return new Model.Library(
                gson.fromJson(o.get("sourceType"), ClassName.class),
                gson.fromJson(o.get("glueType"), ClassName.class),
                o.get("moduleName").getAsString(),
                LuaLibrary.Scope.REQUIRE,
                gson.fromJson(o.get("exports"), EXPORT_LIST),
                gson.fromJson(o.get("staticMethods"), METHOD_LIST),
                gson.fromJson(o.get("staticProperties"), PROPERTY_LIST),
                o.get("description").getAsString());
        }
    }

    /// Export with `luaName` published as `name`.
    private static final class ExportAdapter extends TypeAdapter<Model.Export> {
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
            o.addProperty("description", v.description());
            gson.toJson(o, out);
        }

        @Override
        public Model.Export read(JsonReader in) {
            JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
            return new Model.Export(
                gson.fromJson(o.get("javaType"), TypeName.class),
                o.get("name").getAsString(),
                gson.fromJson(o.get("superExport"), TypeName.class),
                o.get("isFinal").getAsBoolean(),
                gson.fromJson(o.get("generics"), GENERIC_LIST),
                gson.fromJson(o.get("properties"), PROPERTY_LIST),
                gson.fromJson(o.get("methods"), METHOD_LIST),
                gson.fromJson(o.get("metaMethods"), META_LIST),
                o.get("userDataTag").getAsInt(),
                o.get("hasSubtypes").getAsBoolean(),
                o.get("description").getAsString());
        }
    }

    /// Property with `luaName` published as `name`.
    private static final class PropertyAdapter extends TypeAdapter<Model.Property> {
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

        @Override
        public Model.Property read(JsonReader in) {
            JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
            return new Model.Property(
                o.get("name").getAsString(),
                gson.fromJson(o.get("getter"), Model.Accessor.class),
                gson.fromJson(o.get("setter"), Model.Accessor.class));
        }
    }

    /// Accessor without `javaMethodName` / `enclosingType` (Java-only). Read-back uses an empty
    /// method name and null enclosing type — no JSON consumer reads them.
    private static final class AccessorAdapter extends TypeAdapter<Model.Accessor> {
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

        @Override
        public Model.Accessor read(JsonReader in) {
            JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
            return new Model.Accessor(
                "", null,
                o.get("description").getAsString(),
                o.get("paramName").isJsonNull() ? null : o.get("paramName").getAsString(),
                gson.fromJson(o.get("type"), LuauType.class));
        }
    }

    /// Method with `luaName` published as `name`; `javaMethodName` / `enclosingType` / `isVoid`
    /// dropped. Read-back derives `isVoid` from an empty `returns` and leaves the Java fields
    /// blank — only the annotation processor needs them, and it works off the live model.
    private static final class MethodAdapter extends TypeAdapter<Model.Method> {
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

        @Override
        public Model.Method read(JsonReader in) {
            JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
            List<Model.Return> returns = gson.fromJson(o.get("returns"), RETURN_LIST);
            return new Model.Method(
                o.get("name").getAsString(),
                "", returns.isEmpty(), null,
                o.get("description").getAsString(),
                gson.fromJson(o.get("generics"), GENERIC_LIST),
                gson.fromJson(o.get("params"), PARAM_LIST),
                returns);
        }
    }

    /// Meta-method keyed by its Lua metamethod (`__add`, …); `javaMethodName` / `isVoid`
    /// dropped, same rationale as [MethodAdapter].
    private static final class MetaMethodAdapter extends TypeAdapter<Model.MetaMethod> {
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

        @Override
        public Model.MetaMethod read(JsonReader in) {
            JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
            List<Model.Return> returns = gson.fromJson(o.get("returns"), RETURN_LIST);
            return new Model.MetaMethod(
                o.get("meta").getAsString(),
                "", returns.isEmpty(),
                o.get("description").getAsString(),
                gson.fromJson(o.get("generics"), GENERIC_LIST),
                gson.fromJson(o.get("params"), PARAM_LIST),
                returns);
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

    private static final class LuauTypeAdapter extends TypeAdapter<LuauType> {
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
                    o.add("args", gson.toJsonTree(n.args(), new TypeToken<List<LuauType.TypeArg>>() {}.getType()));
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
                    o.add("alternatives", gson.toJsonTree(u.alternatives(), new TypeToken<List<LuauType>>() {}.getType()));
                }
                case LuauType.Intersection it -> {
                    o.addProperty("kind", "intersection");
                    o.add("conjuncts", gson.toJsonTree(it.conjuncts(), new TypeToken<List<LuauType>>() {}.getType()));
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
                    o.add("returns", gson.toJsonTree(f.returns(), new TypeToken<List<LuauType>>() {}.getType()));
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

        @Override
        public LuauType read(JsonReader in) throws IOException {
            JsonElement el = JsonDeserializeHelper.parse(in);
            JsonObject o = el.getAsJsonObject();
            String kind = o.get("kind").getAsString();
            return switch (kind) {
                case "named" -> {
                    String module = o.get("module").isJsonNull() ? null : o.get("module").getAsString();
                    String name = o.get("name").getAsString();
                    List<LuauType.TypeArg> args = gson.fromJson(o.get("args"),
                        new TypeToken<List<LuauType.TypeArg>>() {}.getType());
                    yield new LuauType.Named(module, name, args == null ? List.of() : args);
                }
                case "genericRef" -> new LuauType.GenericRef(o.get("name").getAsString());
                case "optional" -> new LuauType.Optional(gson.fromJson(o.get("inner"), LuauType.class));
                case "union" -> new LuauType.Union(gson.fromJson(o.get("alternatives"),
                    new TypeToken<List<LuauType>>() {}.getType()));
                case "intersection" -> new LuauType.Intersection(gson.fromJson(o.get("conjuncts"),
                    new TypeToken<List<LuauType>>() {}.getType()));
                case "table" -> readTable(o);
                case "function" -> readFunction(o);
                case "variadic" -> new LuauType.Variadic(gson.fromJson(o.get("element"), LuauType.class));
                case "genericPack" -> new LuauType.GenericPack(o.get("name").getAsString());
                case "typeof" -> new LuauType.TypeOf(o.get("expr").getAsString());
                case "string" -> new LuauType.StringLiteral(o.get("value").getAsString());
                case "bool" -> new LuauType.BoolLiteral(o.get("value").getAsBoolean());
                default -> throw new IOException("Unknown LuauType kind: " + kind);
            };
        }

        private LuauType.Table readTable(JsonObject o) {
            var fieldsArr = o.getAsJsonArray("fields");
            var fields = new ArrayList<LuauType.TableField>(fieldsArr.size());
            for (var fEl : fieldsArr) {
                var fo = fEl.getAsJsonObject();
                fields.add(new LuauType.TableField(
                    fo.get("name").getAsString(),
                    gson.fromJson(fo.get("type"), LuauType.class)));
            }
            LuauType arrayElem = o.get("arrayElement").isJsonNull() ? null
                : gson.fromJson(o.get("arrayElement"), LuauType.class);
            LuauType key = o.get("indexerKey").isJsonNull() ? null
                : gson.fromJson(o.get("indexerKey"), LuauType.class);
            LuauType val = o.get("indexerValue").isJsonNull() ? null
                : gson.fromJson(o.get("indexerValue"), LuauType.class);
            return new LuauType.Table(fields, arrayElem, key, val);
        }

        private LuauType.Function readFunction(JsonObject o) {
            var paramsArr = o.getAsJsonArray("params");
            var params = new ArrayList<LuauType.Param>(paramsArr.size());
            for (var pEl : paramsArr) {
                var po = pEl.getAsJsonObject();
                String name = po.get("name").isJsonNull() ? null : po.get("name").getAsString();
                params.add(new LuauType.Param(name, gson.fromJson(po.get("type"), LuauType.class)));
            }
            LuauType.Variadic varargs = o.get("varargs").isJsonNull() ? null
                : gson.fromJson(o.get("varargs"), LuauType.Variadic.class);
            List<LuauType> returns = gson.fromJson(o.get("returns"),
                new TypeToken<List<LuauType>>() {}.getType());
            return new LuauType.Function(params, varargs, returns == null ? List.of() : returns);
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

    private static final class TypeArgAdapter extends TypeAdapter<LuauType.TypeArg> {
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
                    o.add("types", gson.toJsonTree(p.types(), new TypeToken<List<LuauType>>() {}.getType()));
                    o.add("tail", p.tail() == null ? null : gson.toJsonTree(p.tail(), LuauType.Variadic.class));
                }
            }
            gson.toJson(o, out);
        }

        @Override
        public LuauType.TypeArg read(JsonReader in) throws IOException {
            JsonObject o = JsonDeserializeHelper.parse(in).getAsJsonObject();
            String kind = o.get("kind").getAsString();
            return switch (kind) {
                case "single" -> new LuauType.TypeArg.Single(gson.fromJson(o.get("type"), LuauType.class));
                case "pack" -> {
                    List<LuauType> types = gson.fromJson(o.get("types"),
                        new TypeToken<List<LuauType>>() {}.getType());
                    LuauType.Variadic tail = o.get("tail").isJsonNull() ? null
                        : gson.fromJson(o.get("tail"), LuauType.Variadic.class);
                    yield new LuauType.TypeArg.Pack(types == null ? List.of() : types, tail);
                }
                default -> throw new IOException("Unknown TypeArg kind: " + kind);
            };
        }
    }

    // Unused parameters silence the warnings for context objects we don't touch.
    @SuppressWarnings("unused")
    private record Unused(JsonSerializationContext s, JsonDeserializationContext d) {}

    private static final class JsonDeserializeHelper {
        static JsonElement parse(JsonReader in) throws IOException {
            return com.google.gson.JsonParser.parseReader(in);
        }
    }
}
