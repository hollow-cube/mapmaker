package net.hollowcube.anticheat.log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/// The JSON header of a capture trace: everything needed to index, triage or refuse a trace
/// without decompressing its body.
///
/// It is JSON rather than a packed struct on purpose — the store indexes it straight out of an
/// HTTP header and shell tooling reads it with `head -c` and `jq` — so every field is optional as
/// far as parsing goes: an older or newer writer's header still loads, with unknown fields dropped
/// and missing ones null.
public record TraceHeader(
    int formatVersion,
    /// Which [TraceDictionary] the body was compressed against; 0 for none, which is what every
    /// header written before dictionaries existed parses as.
    int dictionaryId,
    int clientPvn,
    @Nullable String brand,
    @Nullable UUID playerId,
    @Nullable String playerName,
    /// Identifies the connection the tap saw, so several traces from one login can be ordered.
    @Nullable String connectionId,
    /// Assigned by the backend (the run id, for competes), null for ring flushes it never asked for.
    @Nullable String captureId,
    @Nullable Reason reason,
    @Nullable ClosedBy closedBy,
    @Nullable Cohort cohort,
    /// The trim actually applied, which is not necessarily the one the backend asked for.
    @Nullable Trim trim,
    @Nullable String proxy,
    @Nullable String proxyVersion,
    @Nullable Instant startedAt,
    @Nullable Instant endedAt,
    @Nullable PingIdRange pingIds,
    Flags flags,
    Counters counters,
    /// Fields a later build wants in the header before the format version is worth bumping.
    Map<String, String> extras
) {

    /// The module's one gson, the control channel included, so a header and a control message
    /// spell the same enum the same way.
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantAdapter().nullSafe())
        .create();

    public TraceHeader {
        flags = Objects.requireNonNullElse(flags, Flags.NONE);
        counters = Objects.requireNonNullElse(counters, Counters.EMPTY);
        extras = extras == null ? Map.of() : Map.copyOf(extras);
    }

    /// Why the capture exists. Sampling and flags are both routine; `manual` is a staff request.
    @RuntimeGson
    public enum Reason {
        @SerializedName("run") RUN,
        @SerializedName("sample") SAMPLE,
        @SerializedName("flag") FLAG,
        @SerializedName("manual") MANUAL
    }

    /// What ended the capture. `superseded` means a second `start` arrived while this one was open;
    /// `flush` is the ring buffer being shipped, which ends nothing — the connection carries on;
    /// `switched` is the player moving to another backend, which ends the run the capture was
    /// opened for while leaving the client connection, and so the ping fence, intact.
    public enum ClosedBy {
        @SerializedName("stop") STOP,
        @SerializedName("disconnect") DISCONNECT,
        @SerializedName("shutdown") SHUTDOWN,
        @SerializedName("superseded") SUPERSEDED,
        @SerializedName("switched") SWITCHED,
        @SerializedName("flush") FLUSH
    }

    /// Sampling cohort. A prior for the grader, never ground truth.
    @RuntimeGson
    public enum Cohort {
        @SerializedName("trusted") TRUSTED,
        @SerializedName("random") RANDOM
    }

    /// `chunkRadius == -1` is "keep everything".
    public record Trim(int chunkRadius, int entityRange) {
    }

    /// The first and last ping id injected during the capture, inclusive. Ids are the tap's
    /// sequence, counted from 1; a trace with no injected ping has none of these at all.
    public record PingIdRange(int first, int last) {
    }

    /// Every way a trace can be less than the whole truth. [#tailUnfenced] means the connection
    /// ended with this trace (disconnect, transfer or proxy shutdown), so the frames after the
    /// last answered ping have no client-side upper bound and never will — terminal frames are
    /// deliberately not fenced, because a ping written right before a kick may never be answered.
    public record Flags(boolean ringTruncated, boolean spoolTruncated, boolean installedMidSession,
                        boolean tailUnfenced) {

        public static final Flags NONE = new Flags(false, false, false, false);

        public boolean any() {
            return ringTruncated || spoolTruncated || installedMidSession || tailUnfenced;
        }

        public Flags withSpoolTruncated(boolean spoolTruncated) {
            return new Flags(ringTruncated, spoolTruncated, installedMidSession, tailUnfenced);
        }
    }

    /// Counted by [TraceWriter] as it writes, except [#droppedFrames] which only the tap can know.
    ///
    /// [#frames]/[#bytes] cover the body's frame section; the prelude is counted separately because
    /// it is synthesized state, not what the connection actually carried.
    public record Counters(long frames, long bytes, long preludeFrames, int chunks, long droppedFrames) {

        public static final Counters EMPTY = new Counters(0, 0, 0, 0, 0);
    }

    public TraceHeader withCounters(Counters counters) {
        return new TraceHeader(formatVersion, dictionaryId, clientPvn, brand, playerId, playerName,
            connectionId, captureId, reason, closedBy, cohort, trim,
            proxy, proxyVersion, startedAt, endedAt, pingIds, flags, counters,
            extras);
    }

    public TraceHeader withFlags(Flags flags) {
        return new TraceHeader(formatVersion, dictionaryId, clientPvn, brand, playerId, playerName,
            connectionId, captureId, reason, closedBy, cohort, trim,
            proxy, proxyVersion, startedAt, endedAt, pingIds, flags, counters,
            extras);
    }

    public TraceHeader withClose(ClosedBy closedBy, Instant endedAt) {
        return new TraceHeader(formatVersion, dictionaryId, clientPvn, brand, playerId, playerName,
            connectionId, captureId, reason, closedBy, cohort, trim,
            proxy, proxyVersion, startedAt, endedAt, pingIds, flags, counters,
            extras);
    }

    public TraceHeader withDictionary(int dictionaryId) {
        return new TraceHeader(formatVersion, dictionaryId, clientPvn, brand, playerId, playerName,
            connectionId, captureId, reason, closedBy, cohort, trim,
            proxy, proxyVersion, startedAt, endedAt, pingIds, flags, counters,
            extras);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static TraceHeader fromJson(String json) {
        var header = GSON.fromJson(json, TraceHeader.class);
        if (header == null) throw new TraceFormatException("empty trace header");
        return header;
    }

    /// Gson has no `java.time` support, and the alternative — epoch millis — is exactly the field
    /// a human reads out of a header, so timestamps are ISO-8601 UTC.
    private static final class InstantAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            var value = in.nextString();
            try {
                return Instant.parse(value);
            } catch (RuntimeException e) {
                throw new TraceFormatException("bad timestamp in trace header: " + value);
            }
        }
    }
}
