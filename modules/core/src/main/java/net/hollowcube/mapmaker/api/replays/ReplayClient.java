package net.hollowcube.mapmaker.api.replays;

import net.hollowcube.mapmaker.api.HttpClientWrapper;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;

import static net.hollowcube.mapmaker.api.ApiClient.notImplemented;
import static net.hollowcube.mapmaker.api.HttpClientWrapper.query;

public interface ReplayClient {

    String PREAMBLE_CONTENT_TYPE = "application/vnd.hollowcube.replay-preamble";
    String COMMIT_CONTENT_TYPE = "application/vnd.hollowcube.replay-commit";
    String REPLAY_CONTENT_TYPE = "application/vnd.hollowcube.replay";

    default ReplayResource get(String replayId) {
        throw notImplemented();
    }

    default ReplayStream getSegment(String replayId, int segmentIndex) {
        return getSegment(replayId, segmentIndex, null);
    }

    default ReplayStream getSegment(String replayId, int segmentIndex, @Nullable ReplayRange range) {
        throw notImplemented();
    }

    default ReplayStream getCompacted(String replayId) {
        return getCompacted(replayId, null);
    }

    default ReplayStream getCompacted(String replayId, @Nullable ReplayRange range) {
        throw notImplemented();
    }

    default ReplayWriteResult commit(String replayId, ReplayCommitRequest request) {
        throw notImplemented();
    }

    default ReplayWriteResult publishCompacted(String replayId, ReplayCompactionRequest request) {
        throw notImplemented();
    }

    record Noop() implements ReplayClient {
    }

    record Http(HttpClientWrapper http) implements ReplayClient {
        private static final String V4_PREFIX = "/v4/internal/replays";

        @Override
        public ReplayResource get(String replayId) {
            var response = http.doRequest(
                "replays.get",
                HttpRequest.newBuilder()
                    .uri(http.url(V4_PREFIX + "/" + replayId))
                    .GET(),
                HttpResponse.BodyHandlers.ofByteArray()
            );
            http.maybeThrowResponse(response);
            requireContentType(response, PREAMBLE_CONTENT_TYPE);

            return new ReplayResource(
                response.body(),
                requireHeader(response, "etag"),
                parseState(response),
                parseRepresentation(response),
                optionalIntegerHeader(response, "replay-next-segment-index")
            );
        }

        @Override
        public ReplayStream getSegment(String replayId, int segmentIndex, @Nullable ReplayRange range) {
            if (segmentIndex < 0)
                throw new IllegalArgumentException("segment index must not be negative");
            return getStream(
                V4_PREFIX + "/" + replayId + "/stream" + query("segment", segmentIndex),
                range
            );
        }

        @Override
        public ReplayStream getCompacted(String replayId, @Nullable ReplayRange range) {
            return getStream(V4_PREFIX + "/" + replayId + "/stream", range);
        }

        @Override
        public ReplayWriteResult commit(String replayId, ReplayCommitRequest request) {
            var requestBuilder = HttpRequest.newBuilder()
                .uri(http.url(V4_PREFIX + "/" + replayId + "/stream"))
                .header("idempotency-key", request.idempotencyKey().toString())
                .header("replay-preamble-length", Integer.toString(request.preamble().length))
                .header("replay-final", Boolean.toString(request.finished()))
                .header("content-type", COMMIT_CONTENT_TYPE)
                .header("content-digest", contentDigest(request.preamble(), request.segment()));

            if (request.expectedEtag() == null)
                requestBuilder.header("if-none-match", "*");
            else
                requestBuilder.header("if-match", request.expectedEtag());

            if (request.segmentIndex() != null)
                requestBuilder.header("replay-segment-index", Integer.toString(request.segmentIndex()));

            var response = http.doRequest(
                "replays.commit",
                requestBuilder.method(
                    "PATCH",
                    HttpRequest.BodyPublishers.concat(
                        HttpRequest.BodyPublishers.ofByteArray(request.preamble()),
                        HttpRequest.BodyPublishers.ofByteArray(request.segment())
                    )
                ),
                HttpResponse.BodyHandlers.ofByteArray()
            );
            http.maybeThrowResponse(response);
            return parseWriteResult(response);
        }

        @Override
        public ReplayWriteResult publishCompacted(String replayId, ReplayCompactionRequest request) {
            var response = http.doRequest(
                "replays.publishCompacted",
                HttpRequest.newBuilder()
                    .uri(http.url(V4_PREFIX + "/" + replayId + "/stream"))
                    .header("if-match", request.expectedEtag())
                    .header("idempotency-key", request.idempotencyKey().toString())
                    .header("replay-preamble-length", Integer.toString(request.preambleLength()))
                    .header("content-type", REPLAY_CONTENT_TYPE)
                    .header("content-digest", contentDigest(request.recording()))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(request.recording())),
                HttpResponse.BodyHandlers.ofByteArray()
            );
            http.maybeThrowResponse(response);
            return parseWriteResult(response);
        }

        private ReplayStream getStream(String path, @Nullable ReplayRange range) {
            var requestBuilder = HttpRequest.newBuilder()
                .uri(http.url(path))
                .GET();
            if (range != null)
                requestBuilder.header("range", range.headerValue());

            var response = http.doRequest(
                "replays.getStream",
                requestBuilder,
                HttpResponse.BodyHandlers.ofByteArray()
            );
            http.maybeThrowResponse(response);

            if (range == null && response.statusCode() != 200)
                throw new IllegalStateException("Expected full replay stream response, got " + response.statusCode());
            if (range != null && response.statusCode() != 206)
                throw new IllegalStateException("Expected ranged replay stream response, got " + response.statusCode());

            long offset;
            long totalLength;
            if (response.statusCode() == 206) {
                var contentRange = parseContentRange(requireHeader(response, "content-range"));
                offset = contentRange.start();
                totalLength = contentRange.totalLength();
                if (response.body().length != contentRange.length())
                    throw new IllegalStateException("Replay range body does not match Content-Range");
            } else {
                offset = 0;
                totalLength = optionalLongHeader(response, "content-length", response.body().length);
            }

            return new ReplayStream(
                response.body(),
                requireHeader(response, "etag"),
                offset,
                totalLength
            );
        }

        private static ContentRange parseContentRange(String value) {
            if (!value.startsWith("bytes "))
                throw new IllegalStateException("Invalid replay content range: " + value);

            int dash = value.indexOf('-', 6);
            int slash = value.indexOf('/', dash + 1);
            if (dash == -1 || slash == -1 || slash == value.length() - 1)
                throw new IllegalStateException("Invalid replay content range: " + value);

            try {
                long start = Long.parseLong(value.substring(6, dash));
                long end = Long.parseLong(value.substring(dash + 1, slash));
                long totalLength = Long.parseLong(value.substring(slash + 1));
                if (start < 0 || end < start || end >= totalLength)
                    throw new IllegalStateException("Invalid replay content range: " + value);
                return new ContentRange(start, end, totalLength);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid replay content range: " + value, e);
            }
        }

        private record ContentRange(long start, long endInclusive, long totalLength) {

            long length() {
                return endInclusive - start + 1;
            }
        }
    }

    private static ReplayWriteResult parseWriteResult(HttpResponse<?> response) {
        return new ReplayWriteResult(
            requireHeader(response, "etag"),
            parseState(response),
            parseRepresentation(response),
            optionalIntegerHeader(response, "replay-next-segment-index")
        );
    }

    private static ReplayState parseState(HttpResponse<?> response) {
        return switch (requireHeader(response, "replay-state").toLowerCase(Locale.ROOT)) {
            case "recording" -> ReplayState.RECORDING;
            case "finished" -> ReplayState.FINISHED;
            case String value -> throw new IllegalStateException("Unknown replay state: " + value);
        };
    }

    private static ReplayRepresentation parseRepresentation(HttpResponse<?> response) {
        return switch (requireHeader(response, "replay-representation").toLowerCase(Locale.ROOT)) {
            case "segmented" -> ReplayRepresentation.SEGMENTED;
            case "compacted" -> ReplayRepresentation.COMPACTED;
            case String value -> throw new IllegalStateException("Unknown replay representation: " + value);
        };
    }

    private static void requireContentType(HttpResponse<?> response, String expected) {
        var contentType = requireHeader(response, "content-type");
        var separator = contentType.indexOf(';');
        var mediaType = separator == -1 ? contentType : contentType.substring(0, separator);
        if (!expected.equalsIgnoreCase(mediaType.trim()))
            throw new IllegalStateException("Unexpected replay content type: " + contentType);
    }

    private static String requireHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name)
            .orElseThrow(() -> new IllegalStateException("Replay response is missing " + name));
    }

    private static @Nullable Integer optionalIntegerHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name)
            .map(value -> parseIntegerHeader(name, value))
            .orElse(null);
    }

    private static long optionalLongHeader(HttpResponse<?> response, String name, long fallback) {
        return response.headers().firstValue(name)
            .map(value -> parseLongHeader(name, value))
            .orElse(fallback);
    }

    private static int parseIntegerHeader(String name, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Replay response has invalid " + name + ": " + value, e);
        }
    }

    private static long parseLongHeader(String name, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Replay response has invalid " + name + ": " + value, e);
        }
    }

    private static String contentDigest(byte[]... parts) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is unavailable", e);
        }

        for (var part : parts)
            digest.update(part);
        return "sha-256=:" + Base64.getEncoder().encodeToString(digest.digest()) + ":";
    }
}
