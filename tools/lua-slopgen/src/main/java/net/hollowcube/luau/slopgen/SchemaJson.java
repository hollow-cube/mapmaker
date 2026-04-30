package net.hollowcube.luau.slopgen;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.slopgen.types.LuauType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/// Single source of truth for translating [Schema] / [Model] / [LuauType] documents to and from
/// JSON. The emitted shape is the canonical published form: per-library fragments use the same
/// schema as the aggregated engine API, just with one entry in `libraries`.
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
        .registerTypeAdapterFactory(new LuauTypeAdapterFactory())
        .registerTypeAdapterFactory(new TypeArgAdapterFactory())
        .create();

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
