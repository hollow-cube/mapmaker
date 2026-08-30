package net.hollowcube.ipc.gen.wire;

import net.hollowcube.ipc.gen.wire.WireDescriptor.Use;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// What a client built against one descriptor cannot survive in a server built against another.
///
/// The old descriptor is the client's: it was built from a release tag, and it is what still runs
/// while the api-server is redeployed from main. So the question is always asked one way — can
/// what the old side sends still be read, and does what it reads still arrive — and every answer
/// is a [Break] naming the wire path that changed.
public final class WireDiff {

    public record Break(String path, String what) {
        @Override
        public String toString() {
            return path + ": " + what;
        }
    }

    public static List<Break> diff(WireDescriptor old, WireDescriptor current) {
        var out = new ArrayList<Break>();

        old.services().forEach((route, oldService) -> {
            var service = current.services().get(route);
            if (service == null) {
                out.add(new Break("service " + route, "removed"));
                return;
            }
            oldService.methods().forEach((name, oldMethod) -> {
                var path = "service " + route + " / method " + name;
                var method = service.methods().get(name);
                if (method == null) {
                    out.add(new Break(path, "removed"));
                    return;
                }
                params(out, path, oldMethod.params(), method.params());
                returns(out, path, oldMethod.returns(), method.returns());
            });
        });

        old.types().forEach((name, oldType) -> {
            var type = current.types().get(name);
            // A type nothing reaches any more is not on the wire; one that still is has every
            // reference to it compared by name.
            if (type == null) return;
            var path = oldType.kind().key() + " " + name;
            if (type.kind() != oldType.kind()) {
                out.add(new Break(path, "became a " + type.kind().key()));
                return;
            }
            switch (oldType.kind()) {
                case RECORD -> fields(out, path, oldType, type);
                case ENUM -> {
                    for (var constant : oldType.constants()) {
                        if (!type.constants().contains(constant)) out.add(new Break(path + " / constant " + constant, "removed"));
                    }
                }
                case SEALED -> {
                    if (!Objects.equals(oldType.discriminator(), type.discriminator())) {
                        out.add(new Break(path, "discriminator " + oldType.discriminator() + " -> " + type.discriminator()));
                    }
                    oldType.variants().forEach((variant, record) -> {
                        var now = type.variants().get(variant);
                        if (now == null) out.add(new Break(path + " / variant " + variant, "removed"));
                        else if (!now.equals(record)) out.add(new Break(path + " / variant " + variant, "type " + record + " -> " + now));
                    });
                }
            }
        });

        keyed(out, "subject", old.subjects(), current.subjects());
        keyed(out, "notification", old.notifications(), current.notifications());
        return out;
    }

    private static void params(List<Break> out, String path, List<WireDescriptor.Field> oldParams,
                               List<WireDescriptor.Field> params) {
        var now = byName(params);
        for (var oldParam : oldParams) {
            var param = now.remove(oldParam.name());
            var paramPath = path + " / param " + oldParam.name();
            if (param == null) continue; // A parameter the server no longer reads is one the old client sends for nothing.
            if (!param.type().equals(oldParam.type())) {
                out.add(new Break(paramPath, "type " + oldParam.type() + " -> " + param.type()));
            } else if (param.nullable() && !oldParam.nullable()) {
                out.add(new Break(paramPath, "non-null -> nullable; an old client does not expect null"));
            } else if (!param.nullable() && oldParam.nullable()) {
                out.add(new Break(paramPath, "nullable -> non-null; an old client may still send null"));
            }
        }
        for (var param : now.values()) {
            if (!param.nullable()) {
                out.add(new Break(path + " / param " + param.name(), "non-null param added; an old client does not send it"));
            }
        }
    }

    private static void returns(List<Break> out, String path, @Nullable WireDescriptor.Slot old,
                                @Nullable WireDescriptor.Slot now) {
        var returnsPath = path + " / returns";
        if (old == null && now == null) return;
        if (old == null || now == null) {
            out.add(new Break(returnsPath, (old == null ? "void" : old.type()) + " -> " + (now == null ? "void" : now.type())));
            return;
        }
        if (!old.type().equals(now.type())) {
            out.add(new Break(returnsPath, "type " + old.type() + " -> " + now.type()));
        } else if (now.nullable() && !old.nullable()) {
            out.add(new Break(returnsPath, "non-null -> nullable; an old client does not expect null"));
        }
        // nullable -> non-null is the server promising more, which an old client is fine with.
    }

    /// Fields are compared knowing who writes the record. What an old client only reads may gain
    /// any field and may tighten a nullable one; what an old client writes — a request, a message,
    /// a notification body — may only gain nullable fields, because the old side will never send
    /// the new one. A record whose use is not recorded is held to both rules.
    private static void fields(List<Break> out, String path, WireDescriptor.Type oldType, WireDescriptor.Type type) {
        var oldWrites = oldType.used().isEmpty() || oldType.usedAs(Use.REQUEST) || oldType.usedAs(Use.MESSAGE) || oldType.usedAs(Use.BODY);
        var oldReads = oldType.used().isEmpty() || oldType.usedAs(Use.RESPONSE) || oldType.usedAs(Use.MESSAGE) || oldType.usedAs(Use.BODY);

        var now = byName(type.fields());
        for (var oldField : oldType.fields()) {
            var field = now.remove(oldField.name());
            var fieldPath = path + " / field " + oldField.name();
            if (field == null) {
                out.add(new Break(fieldPath, "removed"));
            } else if (!field.type().equals(oldField.type())) {
                out.add(new Break(fieldPath, "type " + oldField.type() + " -> " + field.type()));
            } else if (field.nullable() && !oldField.nullable() && oldReads) {
                out.add(new Break(fieldPath, "non-null -> nullable; an old client does not expect null"));
            } else if (!field.nullable() && oldField.nullable() && oldWrites) {
                out.add(new Break(fieldPath, "nullable -> non-null; an old client may still send null"));
            }
        }
        for (var field : now.values()) {
            if (!field.nullable() && oldWrites) {
                out.add(new Break(path + " / field " + field.name(),
                    "non-null field added to a record an old client writes; it has to be nullable"));
            }
        }
    }

    private static void keyed(List<Break> out, String what, Map<String, String> old, Map<String, String> current) {
        old.forEach((key, record) -> {
            var now = current.get(key);
            if (now == null) out.add(new Break(what + " " + key, "removed"));
            else if (!now.equals(record)) out.add(new Break(what + " " + key, "type " + record + " -> " + now));
        });
    }

    private static Map<String, WireDescriptor.Field> byName(List<WireDescriptor.Field> fields) {
        var out = new LinkedHashMap<String, WireDescriptor.Field>();
        for (var field : fields) out.put(field.name(), field);
        return out;
    }

    private WireDiff() {
    }
}
