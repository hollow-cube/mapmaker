package net.hollowcube.apiserver.s3;

import com.sun.net.httpserver.HttpExchange;
import io.opentelemetry.api.OpenTelemetry;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.time.Instant;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The signer against AWS' own published vector, and the five operations against a server that
/// answers like S3 does.
///
/// SigV4 cannot be checked by a fake — a fake accepts whatever it is sent — so the signature is
/// pinned to the example in the AWS documentation instead, and the rest of the client is checked
/// for the things a real bucket would reject it for: the path it addresses, the headers it signs,
/// and what it does with the answer.
class HttpS3ClientTest {

    /// The credentials in AWS' "Signature Calculations for the Authorization Header" examples.
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final Instant FIXED = Instant.parse("2013-05-24T00:00:00Z");

    private HttpServer server;
    private FakeS3 fake;
    private HttpS3Client s3;

    @BeforeEach
    void start() throws IOException {
        fake = new FakeS3();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", fake);
        server.start();
        s3 = new HttpS3Client(HttpClient.newHttpClient(), OpenTelemetry.noop(),
            "http://127.0.0.1:" + server.getAddress().getPort(),
            "mapmaker-replays", "us-east-1", ACCESS_KEY, SECRET_KEY, () -> FIXED);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    /// The GET-Object example from AWS' docs, fed through the half of the signer that is not
    /// request assembly. A failure here is the crypto; a failure anywhere else is the strings.
    @Test
    void signature_matchesTheAwsExample() {
        var canonicalRequest = """
            GET
            /test.txt

            host:examplebucket.s3.amazonaws.com
            range:bytes=0-9
            x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
            x-amz-date:20130524T000000Z

            host;range;x-amz-content-sha256;x-amz-date
            e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855""";

        assertEquals("f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41",
            s3.signature(canonicalRequest, "20130524T000000Z",
                "20130524/us-east-1/s3/aws4_request", "20130524"));
    }

    @Test
    void put_addressesThePathStyleKeyAndSignsTheThreeHeaders() {
        s3.put("replays/ab/segments/0/cd", new ByteArrayInputStream("frames".getBytes(StandardCharsets.UTF_8)), 6);

        var request = fake.requests.getFirst();
        assertEquals("PUT", request.method());
        assertEquals("/mapmaker-replays/replays/ab/segments/0/cd", request.path());
        assertEquals("UNSIGNED-PAYLOAD", request.headers().get("X-Amz-Content-Sha256"));
        assertEquals("20130524T000000Z", request.headers().get("X-Amz-Date"));
        var authorization = request.headers().get("Authorization");
        assertNotNull(authorization);
        assertTrue(authorization.startsWith(
                "AWS4-HMAC-SHA256 Credential=" + ACCESS_KEY + "/20130524/us-east-1/s3/aws4_request,"
                    + " SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature="),
            authorization);
        assertArrayEquals("frames".getBytes(StandardCharsets.UTF_8), fake.objects.get("replays/ab/segments/0/cd"));
    }

    @Test
    void get_readsTheObjectBackWithItsLength() throws IOException {
        fake.objects.put("replays/ab/compacted/cd", "a compacted replay".getBytes(StandardCharsets.UTF_8));

        try (var blob = s3.get("replays/ab/compacted/cd")) {
            assertEquals(18, blob.length());
            assertEquals("a compacted replay", new String(blob.stream().readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void getRange_asksForTheInclusiveRangeAndGetsExactlyIt() throws IOException {
        fake.objects.put("k", "0123456789".getBytes(StandardCharsets.UTF_8));

        try (var blob = s3.getRange("k", 2, 5)) {
            assertEquals(4, blob.length());
            assertEquals("2345", new String(blob.stream().readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals("bytes=2-5", fake.requests.getFirst().headers().get("Range"));
    }

    @Test
    void get_ofAKeyNothingIsUnderIsNotFound() {
        assertThrows(S3Client.NotFoundError.class, () -> s3.get("nothing"));
    }

    @Test
    void delete_removesTheKeyAndDoesNotMindOneThatWasNotThere() {
        fake.objects.put("k", new byte[] {1});

        s3.delete("k");
        s3.delete("k");

        assertEquals(Map.of(), fake.objects);
    }

    /// Two pages, because the continuation token is the part a single-page fake would never
    /// exercise and the orphan sweep this exists for reads hundreds of thousands of keys.
    @Test
    void list_followsTheContinuationTokenAcrossPages() {
        for (var i = 0; i < 5; i++) fake.objects.put("replays/" + i, new byte[] {(byte) i});
        fake.objects.put("other/1", new byte[] {9});
        fake.pageSize = 2;

        assertEquals(List.of("replays/0", "replays/1", "replays/2", "replays/3", "replays/4"),
            s3.list("replays/"));
    }

    private static final class FakeS3 implements HttpHandler {
        private final Map<String, byte[]> objects = new LinkedHashMap<>();
        private final List<Request> requests = new ArrayList<>();
        private int pageSize = 1000;

        private record Request(String method, String path, Map<String, String> headers) {
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // com.sun.net.httpserver normalises header names to its own capitalisation, so the
            // assertions read them back by whatever case they were sent in.
            var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, values.getFirst()));
            var path = exchange.getRequestURI().getPath();
            requests.add(new Request(exchange.getRequestMethod(), path, headers));

            try (exchange) {
                var query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("list-type=2")) {
                    list(exchange, query);
                    return;
                }

                var key = path.substring("/mapmaker-replays/".length());
                switch (exchange.getRequestMethod()) {
                    case "PUT" -> {
                        objects.put(key, exchange.getRequestBody().readAllBytes());
                        exchange.sendResponseHeaders(200, -1);
                    }
                    case "DELETE" -> {
                        objects.remove(key);
                        exchange.sendResponseHeaders(204, -1);
                    }
                    case "GET" -> get(exchange, key, headers.get("Range"));
                    default -> exchange.sendResponseHeaders(405, -1);
                }
            }
        }

        private void get(HttpExchange exchange, String key, @Nullable String range) throws IOException {
            var object = objects.get(key);
            if (object == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            var body = object;
            var status = 200;
            if (range != null) {
                var bounds = range.substring("bytes=".length()).split("-");
                var start = Integer.parseInt(bounds[0]);
                var end = Math.min(Integer.parseInt(bounds[1]), object.length - 1);
                body = Arrays.copyOfRange(object, start, end + 1);
                status = 206;
            }
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }

        private void list(HttpExchange exchange, String query) throws IOException {
            var params = new LinkedHashMap<String, String>();
            for (var pair : query.split("&")) {
                var split = pair.indexOf('=');
                params.put(pair.substring(0, split),
                    URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8));
            }

            var matching = objects.keySet().stream()
                .filter(key -> key.startsWith(params.getOrDefault("prefix", "")))
                .sorted()
                .toList();
            var from = params.containsKey("continuation-token")
                ? matching.indexOf(params.get("continuation-token"))
                : 0;
            var to = Math.min(from + pageSize, matching.size());

            var body = new StringBuilder("<ListBucketResult>");
            for (var key : matching.subList(from, to)) body.append("<Contents><Key>").append(key).append("</Key></Contents>");
            body.append("<IsTruncated>").append(to < matching.size()).append("</IsTruncated>");
            if (to < matching.size())
                body.append("<NextContinuationToken>").append(matching.get(to)).append("</NextContinuationToken>");
            body.append("</ListBucketResult>");

            var bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
