package net.hollowcube.ipc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

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

    /// Header the arguments of a call whose body is a [Blob] travel in, since the body is the blob.
    /// Everything else about such a call is a call like any other.
    public static final String ARGS_HEADER = "x-ipc-args";

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapterFactory(new WireAdapters())
        .registerTypeAdapter(Instant.class, new InstantAdapter().nullSafe())
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

    /// One call's arguments as an [#ARGS_HEADER] value: the json object they would have been the
    /// body of, escaped to ascii.
    ///
    /// A header field is bytes, and a name or a reason written outside latin-1 would not survive
    /// being one. `\\uXXXX` is json's own escape, so the value stays the same json — and stays
    /// readable in a log for the arguments that are ascii already, which is nearly all of them.
    public static String args(JsonElement args) {
        var json = args.toString();
        var out = new StringBuilder(json.length());
        for (int i = 0; i < json.length(); i++) {
            var c = json.charAt(i);
            if (c < ' ' || c > '~') out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.toString();
    }

    private static String defaultClientVersion() {
        var version = System.getenv("MAPMAKER_VERSION");
        if (version == null || version.isBlank()) version = System.getenv("MAPMAKER_COMMIT_SHA");
        return version == null || version.isBlank() ? "dev" : version;
    }

    /// An [Instant] as ISO-8601, which is the one time format nothing has to be told the unit of.
    ///
    /// Gson has no built-in adapter for it and would otherwise reflect over the record's seconds and
    /// nanos, which is neither readable nor stable.
    private static final class InstantAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            var raw = in.nextString();
            try {
                return Instant.parse(raw);
            } catch (DateTimeParseException e) {
                throw new IOException("not an ISO-8601 instant: " + raw, e);
            }
        }
    }

    private Wire() {
    }
}
