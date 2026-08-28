package net.hollowcube.ipc.gen;

import com.palantir.javapoet.ClassName;

/// The fixed names the generated code is written against.
///
/// `ipc-gen` cannot depend on `ipc` (`ipc` applies this processor), so the runtime types the
/// generated code touches are named rather than referenced.
final class IpcNames {

    static final String IPC_ANNOTATION = "net.hollowcube.ipc.util.Ipc";
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
