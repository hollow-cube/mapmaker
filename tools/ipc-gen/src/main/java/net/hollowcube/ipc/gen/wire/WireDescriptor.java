package net.hollowcube.ipc.gen.wire;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/// Everything about the wire that a client built from one commit and a server built from another
/// have to agree on, as `modules/ipc/wire.json` holds it: services and their methods, every record,
/// enum and sealed type reachable from one, NATS subjects and notification type keys.
///
/// Written and read here rather than through gson's reflection so that the file's shape is spelled
/// out in one place and stays byte-for-byte deterministic: maps are sorted, lists keep declaration
/// order, and `nullable` only appears when it is true.
public record WireDescriptor(
    SortedMap<String, Service> services,
    SortedMap<String, Type> types,
    /// NATS subject to the record published on it.
    SortedMap<String, String> subjects,
    /// Notification `type` key to the record its `data` holds.
    SortedMap<String, String> notifications
) {

    /// @param java the interface, for the reader; the route is what the wire knows
    public record Service(String java, SortedMap<String, Method> methods) {
    }

    /// @param returns null for a `void` method
    public record Method(List<Field> params, @Nullable Slot returns) {
    }

    public record Field(String name, String type, boolean nullable) {
    }

    public record Slot(String type, boolean nullable) {
    }

    /// How a record is reached, which is who writes it: a client sends a request, a server
    /// answers a response, either side publishes a message or stores a body. Declared in the order
    /// they are written, so an `EnumSet` of them is sorted.
    public enum Use {
        BODY, MESSAGE, REQUEST, RESPONSE;

        String key() {
            return name().toLowerCase();
        }

        static Use parse(String key) {
            return valueOf(key.toUpperCase());
        }
    }

    public enum Kind {
        RECORD, ENUM, SEALED;

        String key() {
            return name().toLowerCase();
        }

        static Kind parse(String key) {
            return valueOf(key.toUpperCase());
        }
    }

    /// @param used how a record is reached, which is what decides whether a field added to it
    ///             later is safe
    public record Type(
        Kind kind,
        List<String> typeParameters,
        List<Field> fields,
        List<String> constants,
        @Nullable String discriminator,
        SortedMap<String, String> variants,
        Set<Use> used
    ) {
        public static Type record(List<String> typeParameters, List<Field> fields, Set<Use> used) {
            return new Type(Kind.RECORD, typeParameters, fields, List.of(), null, new TreeMap<>(), used.isEmpty() ? EnumSet.noneOf(Use.class) : EnumSet.copyOf(used));
        }

        public static Type enumeration(List<String> constants) {
            return new Type(Kind.ENUM, List.of(), List.of(), constants, null, new TreeMap<>(), EnumSet.noneOf(Use.class));
        }

        public static Type sealed(String discriminator, SortedMap<String, String> variants) {
            return new Type(Kind.SEALED, List.of(), List.of(), List.of(), discriminator, variants, EnumSet.noneOf(Use.class));
        }

        public boolean usedAs(Use use) {
            return used.contains(use);
        }
    }

    public String toJson() {
        var root = new JsonObject();
        var services = new JsonObject();
        this.services.forEach((route, service) -> {
            var json = new JsonObject();
            json.addProperty("interface", service.java());
            var methods = new JsonObject();
            service.methods().forEach((name, method) -> {
                var methodJson = new JsonObject();
                methodJson.add("params", fields(method.params()));
                if (method.returns() != null) {
                    var returns = new JsonObject();
                    returns.addProperty("type", method.returns().type());
                    if (method.returns().nullable()) returns.addProperty("nullable", true);
                    methodJson.add("returns", returns);
                }
                methods.add(name, methodJson);
            });
            json.add("methods", methods);
            services.add(route, json);
        });
        root.add("services", services);

        var types = new JsonObject();
        this.types.forEach((name, type) -> {
            var json = new JsonObject();
            json.addProperty("kind", type.kind().key());
            if (!type.typeParameters().isEmpty()) json.add("typeParameters", strings(type.typeParameters()));
            if (!type.used().isEmpty()) json.add("used", strings(type.used().stream().map(Use::key).toList()));
            switch (type.kind()) {
                case RECORD -> json.add("fields", fields(type.fields()));
                case ENUM -> json.add("constants", strings(type.constants()));
                case SEALED -> {
                    json.addProperty("discriminator", type.discriminator());
                    var variants = new JsonObject();
                    type.variants().forEach(variants::addProperty);
                    json.add("variants", variants);
                }
            }
            types.add(name, json);
        });
        root.add("types", types);

        var subjects = new JsonObject();
        this.subjects.forEach(subjects::addProperty);
        root.add("subjects", subjects);
        var notifications = new JsonObject();
        this.notifications.forEach(notifications::addProperty);
        root.add("notifications", notifications);

        var out = new StringWriter();
        try (var writer = new JsonWriter(out)) {
            writer.setIndent("  ");
            writer.setHtmlSafe(false);
            new Gson().toJson(root, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out + "\n";
    }

    public static WireDescriptor parse(String json) {
        var root = JsonParser.parseString(json).getAsJsonObject();

        var services = new TreeMap<String, Service>();
        object(root, "services").asMap().forEach((route, element) -> {
            var service = element.getAsJsonObject();
            var methods = new TreeMap<String, Method>();
            object(service, "methods").asMap().forEach((name, methodElement) -> {
                var method = methodElement.getAsJsonObject();
                var returns = method.get("returns");
                methods.put(name, new Method(fields(method.get("params")), returns == null || returns.isJsonNull()
                    ? null
                    : new Slot(returns.getAsJsonObject().get("type").getAsString(), flag(returns.getAsJsonObject()))));
            });
            services.put(route, new Service(string(service, "interface"), methods));
        });

        var types = new TreeMap<String, Type>();
        object(root, "types").asMap().forEach((name, element) -> {
            var type = element.getAsJsonObject();
            var variants = new TreeMap<String, String>();
            object(type, "variants").asMap().forEach((variant, record) -> variants.put(variant, record.getAsString()));
            types.put(name, new Type(
                Kind.parse(string(type, "kind")),
                strings(type.get("typeParameters")),
                fields(type.get("fields")),
                strings(type.get("constants")),
                type.has("discriminator") ? string(type, "discriminator") : null,
                variants,
                uses(type.get("used"))));
        });

        return new WireDescriptor(services, types, stringMap(root, "subjects"), stringMap(root, "notifications"));
    }

    private static JsonArray fields(List<Field> fields) {
        var out = new JsonArray();
        for (var field : fields) {
            var json = new JsonObject();
            json.addProperty("name", field.name());
            json.addProperty("type", field.type());
            if (field.nullable()) json.addProperty("nullable", true);
            out.add(json);
        }
        return out;
    }

    private static JsonArray strings(Iterable<String> values) {
        var out = new JsonArray();
        for (var value : values) out.add(value);
        return out;
    }

    private static List<Field> fields(@Nullable JsonElement element) {
        var out = new ArrayList<Field>();
        if (element == null) return out;
        for (var item : element.getAsJsonArray()) {
            var field = item.getAsJsonObject();
            out.add(new Field(string(field, "name"), string(field, "type"), flag(field)));
        }
        return out;
    }

    private static Set<Use> uses(@Nullable JsonElement element) {
        var out = EnumSet.noneOf(Use.class);
        for (var key : strings(element)) out.add(Use.parse(key));
        return out;
    }

    private static List<String> strings(@Nullable JsonElement element) {
        var out = new ArrayList<String>();
        if (element == null) return out;
        for (var item : element.getAsJsonArray()) out.add(item.getAsString());
        return out;
    }

    private static SortedMap<String, String> stringMap(JsonObject parent, String key) {
        var out = new TreeMap<String, String>();
        object(parent, key).asMap().forEach((name, value) -> out.put(name, value.getAsString()));
        return out;
    }

    private static JsonObject object(JsonObject parent, String key) {
        var value = parent.get(key);
        return value == null ? new JsonObject() : value.getAsJsonObject();
    }

    private static String string(JsonObject parent, String key) {
        return parent.get(key).getAsString();
    }

    private static boolean flag(JsonObject parent) {
        var value = parent.get("nullable");
        return value != null && value.getAsBoolean();
    }
}
