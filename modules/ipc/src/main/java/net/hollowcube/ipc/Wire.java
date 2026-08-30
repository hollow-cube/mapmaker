package net.hollowcube.ipc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/// The one [Gson] every wire value is encoded with, and the version this process announces itself
/// as when it calls another.
///
/// The gson carries [WireAdapters], which `ipc-gen` writes for every enum and sealed interface it
/// found on the wire: an enum constant this build does not know decodes to `UNKNOWN`, a sealed
/// variant it does not know decodes to the interface's `Unknown` record, and neither can be
/// encoded back. Generated clients and servers use it, and so must anything else that puts a wire
/// record on a socket or in a row — a plain `new Gson()` would read a new constant as an exception.
public final class Wire {

    /// Header a generated client sends its [#clientVersion] in, and the server records on its span.
    public static final String CLIENT_HEADER = "x-ipc-client";

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapterFactory(new WireAdapters())
        .create();

    private static volatile String clientVersion = defaultClientVersion();

    public static Gson gson() {
        return GSON;
    }

    /// What this process says it is on every ipc call: the release tag or commit it was built
    /// from, which is how the oldest client still talking to the api-server is known before
    /// `wire-baseline` is moved. Defaults to the `MAPMAKER_VERSION` or `MAPMAKER_COMMIT_SHA`
    /// environment, and `dev` when there is neither.
    public static String clientVersion() {
        return clientVersion;
    }

    public static void setClientVersion(String version) {
        clientVersion = version;
    }

    private static String defaultClientVersion() {
        var version = System.getenv("MAPMAKER_VERSION");
        if (version == null || version.isBlank()) version = System.getenv("MAPMAKER_COMMIT_SHA");
        return version == null || version.isBlank() ? "dev" : version;
    }

    private Wire() {
    }
}
