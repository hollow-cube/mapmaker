package net.hollowcube.ipc.gen;

import com.palantir.javapoet.ClassName;

/// The fixed names the generated code is written against.
///
/// `ipc-gen` cannot depend on `ipc` (`ipc` applies this processor), so the runtime types the
/// generated code touches are named rather than referenced.
final class IpcNames {

    static final String IPC_ANNOTATION = "net.hollowcube.ipc.util.Ipc";
    static final String NATS_MESSAGE_ANNOTATION = "net.hollowcube.ipc.util.NatsMessage";
    static final String NOTIFICATION_BODY_ANNOTATION = "net.hollowcube.ipc.util.NotificationBody";
    /// Lives in `modules:common`, which is `compileOnly` to `ipc`; read by name off the mirror.
    static final String RUNTIME_GSON_ANNOTATION = "net.hollowcube.common.util.RuntimeGson";
    static final String SERIALIZED_NAME_ANNOTATION = "com.google.gson.annotations.SerializedName";
    /// Where sql-gen writes. The schema and the wire are versioned apart, so nothing from here is
    /// allowed at a wire position.
    static final String DB_PACKAGE = "net.hollowcube.apiserver.db";

    static final ClassName WIRE = ClassName.get("net.hollowcube.ipc", "Wire");
    /// The one generated `TypeAdapterFactory`, which `Wire` registers on its gson.
    static final ClassName WIRE_ADAPTERS = ClassName.get("net.hollowcube.ipc", "WireAdapters");

    /// What every wire enum has to declare, and what a constant this build does not know reads as.
    static final String UNKNOWN_CONSTANT = "UNKNOWN";
    /// Suffix of the generated record a sealed wire type's unknown discriminator reads as.
    static final String UNKNOWN_SUFFIX = "Unknown";
    /// Field a sealed wire type's variant is named in.
    static final String DISCRIMINATOR = "type";
    static final ClassName IPC_EXCEPTION = ClassName.get("net.hollowcube.ipc.util", "IpcException");
    static final ClassName IPC_SPAN = ClassName.get("net.hollowcube.ipc.util", "IpcSpan");
    static final ClassName IPC_TRACING = ClassName.get("net.hollowcube.ipc.util", "IpcTracing");

    private static final String SERVICE_SUFFIX = "Service";
    /// Suffix of the generated caller, appended to [#base].
    static final String CLIENT_SUFFIX = "Client";
    /// Suffix of the generated http handler, appended to [#base].
    static final String SERVER_SUFFIX = "Server";

    /// What both generated classes are named after: the interface's simple name without a trailing
    /// `Service`, so `HeadDatabaseService` produces `HeadDatabaseClient` and `HeadDatabaseServer`
    /// and neither name leaves you guessing which side of the wire it is.
    static String base(String simpleName) {
        return simpleName.endsWith(SERVICE_SUFFIX) && !simpleName.equals(SERVICE_SUFFIX)
            ? simpleName.substring(0, simpleName.length() - SERVICE_SUFFIX.length())
            : simpleName;
    }

    /// The route a service is mounted at, in kebab case: `HeadDatabaseService` serves
    /// `/head-database`.
    static String servicePath(String base) {
        return "/" + kebab(base);
    }

    /// The route one method answers on, relative to the service: `getHeads` becomes `get-heads`.
    static String methodPath(String name) {
        return kebab(name);
    }

    /// The discriminator value one sealed variant is written as: its record's simple name in kebab
    /// case, the way a class name becomes a route.
    static String variantName(String simpleName) {
        return kebab(simpleName);
    }

    /// The generated unknown variant of a sealed wire type: a top-level record in the interface's
    /// package, named after every enclosing type so that `Foo.Body` gives `FooBodyUnknown`.
    static ClassName unknownVariant(ClassName sealed) {
        return ClassName.get(sealed.packageName(), String.join("", sealed.simpleNames()) + UNKNOWN_SUFFIX);
    }

    /// Lower-kebab of a Java identifier, breaking before each capital that starts a word. A run of
    /// capitals is one word — `getHDBEntry` is `get-hdb-entry`, not `get-h-d-b-entry` — and digits
    /// stay attached to the word they were written against.
    private static String kebab(String name) {
        var out = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            var c = name.charAt(i);
            if (Character.isUpperCase(c) && !out.isEmpty()
                && (!Character.isUpperCase(name.charAt(i - 1))
                    || (i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1))))) {
                out.append('-');
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }

    private IpcNames() {
    }
}
