package net.hollowcube.apiserver.s3;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.semconv.SemanticAttributes;
import net.hollowcube.apiserver.common.Digest;
import net.hollowcube.ipc.Blob;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/// The S3 API over `java.net.http`, signed with AWS Signature Version 4.
///
/// Hand-rolled rather than the AWS SDK because the api-server builds as a native image and the SDK
/// is a pile of reflection and service metadata for five requests. Path-style, which is what the Go
/// api-server configures and what R2 and MinIO both serve.
///
/// A `PUT` signs `UNSIGNED-PAYLOAD` because hashing the body up front would mean holding it.
/// Integrity is not lost by it: every replay object carries a SHA-256 in its row.
public final class HttpS3Client implements S3Client {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String SERVICE = "s3";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    /// What a request with no body hashes to.
    private static final String EMPTY_PAYLOAD =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final DateTimeFormatter STAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DAY =
        DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private static final AttributeKey<String> BUCKET = AttributeKey.stringKey("aws.s3.bucket");
    private static final AttributeKey<String> KEY = AttributeKey.stringKey("aws.s3.key");

    private final HttpClient http;
    private final Tracer tracer;
    private final URI endpoint;
    private final String bucket;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final Supplier<Instant> clock;

    /// An untraced client, for a caller that has no [OpenTelemetry].
    public HttpS3Client(HttpClient http, String endpoint, String bucket, String region,
                        String accessKey, String secretKey) {
        this(http, OpenTelemetry.noop(), endpoint, bucket, region, accessKey, secretKey, Instant::now);
    }

    public HttpS3Client(HttpClient http, OpenTelemetry otel, String endpoint, String bucket, String region,
                        String accessKey, String secretKey) {
        this(http, otel, endpoint, bucket, region, accessKey, secretKey, Instant::now);
    }

    @TestOnly
    public HttpS3Client(HttpClient http, OpenTelemetry otel, String endpoint, String bucket, String region,
                        String accessKey, String secretKey, Supplier<Instant> clock) {
        this.http = http;
        this.tracer = otel.getTracer("net.hollowcube.apiserver.s3");
        this.endpoint = URI.create(endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint);
        this.bucket = bucket;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.clock = clock;
    }

    @Override
    public void put(String key, InputStream body, long length) {
        var publisher = HttpRequest.BodyPublishers.ofInputStream(() -> body);
        if (length >= 0) publisher = HttpRequest.BodyPublishers.fromPublisher(publisher, length);
        var response = send("put", key,
            request("PUT", key, Map.of(), UNSIGNED_PAYLOAD).PUT(publisher),
            HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) throw new NotFoundError(key);
        require(response, "PUT", key, response.body());
    }

    @Override
    public Blob get(String key) {
        return download(key, null);
    }

    @Override
    public Blob getRange(String key, long start, long endInclusive) {
        if (start < 0 || endInclusive < start)
            throw new IllegalArgumentException("not a byte range: " + start + "-" + endInclusive);
        return download(key, "bytes=" + start + "-" + endInclusive);
    }

    @Override
    public void delete(String key) {
        var response = send("delete", key,
            request("DELETE", key, Map.of(), EMPTY_PAYLOAD).DELETE(),
            HttpResponse.BodyHandlers.ofString());
        // S3 answers 204 for a key that was never there, which is the behaviour a sweep wants.
        if (response.statusCode() == 404) return;
        require(response, "DELETE", key, response.body());
    }

    @Override
    public List<String> list(String prefix) {
        var keys = new ArrayList<String>();
        String token = null;
        do {
            var query = new TreeMap<String, String>();
            query.put("list-type", "2");
            query.put("prefix", prefix);
            if (token != null) query.put("continuation-token", token);

            var response = send("list", prefix,
                request("GET", "", query, EMPTY_PAYLOAD).GET(),
                HttpResponse.BodyHandlers.ofString());
            require(response, "LIST", prefix, response.body());

            var body = response.body();
            keys.addAll(tags(body, "Key"));
            var truncated = tags(body, "IsTruncated");
            token = truncated.isEmpty() || !"true".equals(truncated.getFirst())
                ? null
                : tags(body, "NextContinuationToken").stream().findFirst().orElse(null);
        } while (token != null);
        return List.copyOf(keys);
    }

    /// For a local run against a MinIO that came up empty; the Go api-server does this on startup
    /// too. Not on [S3Client]: in the cluster the bucket exists and the credentials may not be
    /// allowed to make one.
    public void createBucketIfAbsent() {
        var response = send("create-bucket", bucket,
            request("PUT", "", Map.of(), EMPTY_PAYLOAD).PUT(HttpRequest.BodyPublishers.noBody()),
            HttpResponse.BodyHandlers.ofString());
        // BucketAlreadyOwnedByYou, which is every start after the first.
        if (response.statusCode() == 409) return;
        require(response, "CREATE BUCKET", bucket, response.body());
    }

    private Blob download(String key, @Nullable String range) {
        var builder = request("GET", key, Map.of(), EMPTY_PAYLOAD).GET();
        if (range != null) builder.header("Range", range);

        var response = send(range == null ? "get" : "get-range", key, builder,
            HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 404) {
            text(response.body());
            throw new NotFoundError(key);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            var message = text(response.body());
            throw new RequestFailedError("GET " + key + " answered " + response.statusCode() + ": " + message);
        }
        return new Blob(response.headers().firstValueAsLong("content-length").orElse(-1), response.body());
    }

    /// `payloadHash` is what the signature commits the body to, and is a header as well as a field
    /// of the canonical request.
    private HttpRequest.Builder request(String method, String key, Map<String, String> query, String payloadHash) {
        var path = "/" + bucket + (key.isEmpty() ? "" : "/" + key);
        var canonicalPath = encodePath(path);
        var canonicalQuery = encodeQuery(query);
        var url = endpoint + canonicalPath + (canonicalQuery.isEmpty() ? "" : "?" + canonicalQuery);

        var now = clock.get();
        var stamp = STAMP.format(now);
        var day = DAY.format(now);
        var scope = day + "/" + region + "/" + SERVICE + "/aws4_request";

        // Signed but never set: java.net.http derives `host` from the uri, so what is signed here
        // has to be what it will send.
        var host = endpoint.getHost() + (endpoint.getPort() == -1 || endpoint.getPort() == defaultPort()
            ? "" : ":" + endpoint.getPort());
        var canonicalRequest = method + "\n"
            + canonicalPath + "\n"
            + canonicalQuery + "\n"
            + "host:" + host + "\n"
            + "x-amz-content-sha256:" + payloadHash + "\n"
            + "x-amz-date:" + stamp + "\n"
            + "\n"
            + "host;x-amz-content-sha256;x-amz-date\n"
            + payloadHash;

        var signature = signature(canonicalRequest, stamp, scope, day);

        return HttpRequest.newBuilder(URI.create(url))
            .header("x-amz-date", stamp)
            .header("x-amz-content-sha256", payloadHash)
            .header("Authorization", ALGORITHM
                + " Credential=" + accessKey + "/" + scope
                + ", SignedHeaders=host;x-amz-content-sha256;x-amz-date"
                + ", Signature=" + signature);
    }

    /// The whole of SigV4 that is not string assembly.
    String signature(String canonicalRequest, String stamp, String scope, String day) {
        var stringToSign = ALGORITHM + "\n" + stamp + "\n" + scope + "\n"
            + Digest.hex(Digest.sha256(canonicalRequest));
        return Digest.hex(hmac(signingKey(day), stringToSign.getBytes(StandardCharsets.UTF_8)));
    }

    /// The port java.net.http leaves out of the `Host` it derives, and so must not be signed.
    private int defaultPort() {
        return "https".equalsIgnoreCase(endpoint.getScheme()) ? 443 : 80;
    }

    private byte[] signingKey(String day) {
        var key = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), day.getBytes(StandardCharsets.UTF_8));
        key = hmac(key, region.getBytes(StandardCharsets.UTF_8));
        key = hmac(key, SERVICE.getBytes(StandardCharsets.UTF_8));
        return hmac(key, "aws4_request".getBytes(StandardCharsets.UTF_8));
    }

    /// The span ends before the body is read: a [#get] answers with the stream still open, and how
    /// long the caller takes over it is not the store's time.
    private <T> HttpResponse<T> send(String operation, String key, HttpRequest.Builder request,
                                     HttpResponse.BodyHandler<T> handler) {
        var span = tracer.spanBuilder("s3/" + operation).setSpanKind(SpanKind.CLIENT).startSpan();
        span.setAttribute(BUCKET, bucket);
        span.setAttribute(KEY, key);
        try (var ignored = span.makeCurrent()) {
            var response = http.send(request.build(), handler);
            span.setAttribute(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE, (long) response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) span.setStatus(StatusCode.ERROR);
            return response;
        } catch (IOException e) {
            throw failed(span, new RequestFailedError("s3 " + operation + " " + key + " failed: " + e, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failed(span, new RequestFailedError("s3 " + operation + " " + key + " interrupted", e));
        } finally {
            span.end();
        }
    }

    private static RequestFailedError failed(Span span, RequestFailedError error) {
        span.recordException(error);
        span.setStatus(StatusCode.ERROR, String.valueOf(error.getMessage()));
        return error;
    }

    private static void require(HttpResponse<?> response, String what, String key, @Nullable String body) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) return;
        throw new RequestFailedError(what + " " + key + " answered " + response.statusCode() + ": " + body);
    }

    /// Each segment percent-encoded, separators left alone. S3 signs the path once, unlike every
    /// other AWS service, so this is not applied twice.
    private static String encodePath(String path) {
        var segments = path.split("/", -1);
        var out = new StringBuilder(path.length());
        for (var i = 0; i < segments.length; i++) {
            if (i > 0) out.append('/');
            out.append(encode(segments[i]));
        }
        return out.toString();
    }

    private static String encodeQuery(Map<String, String> query) {
        var out = new StringBuilder();
        // A TreeMap at the call site, so already in the byte order the signature wants.
        for (var entry : query.entrySet()) {
            if (!out.isEmpty()) out.append('&');
            out.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return out.toString();
    }

    /// RFC 3986 unreserved, stricter than `URLEncoder`: a space is `%20` rather than `+`, and `*`,
    /// `~` and `/` are not what form encoding makes of them.
    private static String encode(String value) {
        var out = new StringBuilder(value.length());
        for (var b : value.getBytes(StandardCharsets.UTF_8)) {
            var c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || c == '-' || c == '_' || c == '.' || c == '~') {
                out.append(c);
            } else {
                out.append('%').append(HexFormat.of().withUpperCase().toHexDigits(b));
            }
        }
        return out.toString();
    }

    /// A scanner rather than an XML parser: the answer has three fields worth reading, and a parser
    /// in a native image is reflection configuration nobody wants to maintain for it.
    private static List<String> tags(String xml, String tag) {
        var open = "<" + tag + ">";
        var close = "</" + tag + ">";
        var out = new ArrayList<String>();
        var at = xml.indexOf(open);
        while (at >= 0) {
            var start = at + open.length();
            var end = xml.indexOf(close, start);
            if (end < 0) break;
            out.add(unescape(xml.substring(start, end)));
            at = xml.indexOf(open, end + close.length());
        }
        return out;
    }

    private static String unescape(String value) {
        if (value.indexOf('&') < 0) return value;
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&");
    }

    private static String text(InputStream body) {
        try (body) {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unreadable: " + e + ">";
        }
    }

    private static byte[] hmac(byte[] key, byte[] value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AssertionError("HmacSHA256 is unavailable", e);
        }
    }
}
