package net.hollowcube.ipc.gen;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/// Compiles a service, loads the generated classes, and drives one half against the other over a
/// real [HttpServer].
///
/// The source assertions in [IpcProcessorTest] say what the emitters wrote; this says the two
/// halves agree — which is the only property that matters and the one a string route cannot give
/// you. Serialization is where a generated client and a hand-written server drift, so it is checked
/// end to end rather than per side, and so is what the generated adapters do with a constant or a
/// variant one side does not know.
class IpcRoundTripTest {

    private static final JavaFileObject SERVICE = JavaFileObjects.forSourceString("test.EchoService", """
        package test;

        import net.hollowcube.common.util.RuntimeGson;
        import net.hollowcube.ipc.Blob;
        import net.hollowcube.ipc.util.Ipc;
        import org.jetbrains.annotations.Nullable;
        import java.util.List;
        import java.util.Map;

        @Ipc
        public interface EchoService {

            String echo(String message, int count);

            String suffix(String message, @Nullable String suffix);

            List<String> split(String value, String separator);

            Map<String, List<Point>> group(List<Point> points);

            Point move(Point point, int dx);

            Color paint(Color color);

            Shape grow(Shape shape);

            void record(String event);

            String fail();

            String store(String name, Blob body);

            Blob fetch(String name);

            @RuntimeGson
            record Point(int x, int y) {
            }

            enum Color {
                RED, GREEN, UNKNOWN
            }

            sealed interface Shape permits Circle, Square, Shape.Unknown {
                @RuntimeGson
                record Unknown(@Nullable String type) implements Shape {
                }
            }

            @RuntimeGson
            record Circle(int radius) implements Shape {
            }

            @RuntimeGson
            record Square(int side) implements Shape {
            }
        }
        """);

    private static ClassLoader loader;
    private static Class<?> service;
    private static Class<?> point;
    private static Class<?> color;
    private static Class<?> shape;
    private static Class<?> circle;
    private static HttpServer server;
    private static Object client;
    private static List<String> recorded;
    private static List<Object> received;
    private static Class<?> blob;
    private static Map<String, byte[]> stored;

    @BeforeAll
    static void startServer() throws Exception {
        var compilation = compile();
        assertThat(compilation).succeededWithoutWarnings();

        loader = classesOf(compilation);
        service = loader.loadClass("test.EchoService");
        point = loader.loadClass("test.EchoService$Point");
        color = loader.loadClass("test.EchoService$Color");
        shape = loader.loadClass("test.EchoService$Shape");
        circle = loader.loadClass("test.EchoService$Circle");
        blob = loader.loadClass("net.hollowcube.ipc.Blob");
        recorded = new ArrayList<>();
        received = new ArrayList<>();
        stored = new HashMap<>();

        var impl = Proxy.newProxyInstance(loader, new Class<?>[]{service}, (proxy, method, args) ->
            switch (method.getName()) {
                case "echo" -> args[0] + ":" + args[1];
                case "suffix" -> (String) args[0] + (args[1] == null ? "" : args[1]);
                case "split" -> List.of(((String) args[0]).split((String) args[1]));
                case "group" -> Map.of("all", (List<?>) args[0]);
                case "move" -> point.getConstructor(int.class, int.class)
                    .newInstance(x(args[0]) + (int) args[1], y(args[0]));
                case "paint" -> {
                    received.add(args[0]);
                    yield args[0];
                }
                case "grow" -> {
                    received.add(args[0]);
                    yield circle.isInstance(args[0])
                        ? circle.getConstructor(int.class).newInstance((int) circle.getMethod("radius").invoke(args[0]) + 1)
                        : args[0];
                }
                case "record" -> {
                    recorded.add((String) args[0]);
                    yield null;
                }
                case "fail" -> throw ipcException(409, "already exists");
                case "store" -> {
                    var bytes = (byte[]) blob.getMethod("readAllBytes").invoke(args[1]);
                    stored.put((String) args[0], bytes);
                    yield args[0] + ":" + bytes.length;
                }
                case "fetch" -> {
                    var bytes = stored.get((String) args[0]);
                    if (bytes == null) throw ipcException(404, "no blob " + args[0]);
                    yield blob.getMethod("of", byte[].class).invoke(null, (Object) bytes);
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });

        var serverClass = loader.loadClass("test.EchoServer");
        var handler = (HttpHandler) serverClass.getConstructor(service).newInstance(impl);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext((String) serverClass.getField("PATH").get(null), handler);
        server.start();

        var clientClass = loader.loadClass("test.EchoClient");
        client = clientClass.getConstructor(HttpClient.class, String.class)
            .newInstance(HttpClient.newHttpClient(), baseUrl() + "/");
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void callsCarryEveryArgumentByName() throws Exception {
        assertEquals("hi:3", call("echo", new Class<?>[]{String.class, int.class}, "hi", 3));
    }

    @Test
    void nullableArgumentsArriveAsNull() throws Exception {
        assertEquals("hi", call("suffix", new Class<?>[]{String.class, String.class}, "hi", null));
        assertEquals("hi!", call("suffix", new Class<?>[]{String.class, String.class}, "hi", "!"));
    }

    @Test
    void missingNonNullArgumentsAre400() throws Exception {
        var response = post("/echo/echo", "{\"message\":\"hi\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("missing parameter 'count'"), response.body());
    }

    @Test
    void malformedBodiesAre400() throws Exception {
        var response = post("/echo/echo", "{\"message\":\"hi\",\"count\":\"three\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("malformed request"), response.body());
    }

    @Test
    void genericReturnTypesSurviveErasure() throws Exception {
        assertEquals(List.of("a", "b", "c"), call("split", new Class<?>[]{String.class, String.class}, "a,b,c", ","));
    }

    @Test
    void nestedGenericsSurviveErasure() throws Exception {
        var points = List.of(newPoint(1, 2), newPoint(3, 4));
        var grouped = (Map<?, ?>) call("group", new Class<?>[]{List.class}, points);

        assertEquals(points, grouped.get("all"));
    }

    @Test
    void recordsRoundTripInBothDirections() throws Exception {
        assertEquals(newPoint(6, 2), call("move", new Class<?>[]{point, int.class}, newPoint(1, 2), 5));
    }

    @Test
    void enumsRoundTripByName() throws Exception {
        var red = constant("RED");
        assertEquals(red, call("paint", new Class<?>[]{color}, red));

        var raw = post("/echo/paint", "{\"color\":\"GREEN\"}");
        assertEquals(200, raw.statusCode());
        assertEquals("\"GREEN\"", raw.body());
    }

    /// A constant this build does not know arrives as UNKNOWN rather than as an exception; the
    /// implementation that tries to send it back is the one that fails, so it never leaks.
    @Test
    void unknownEnumConstantsReadAsUnknownAndCannotBeWritten() throws Exception {
        var response = post("/echo/paint", "{\"color\":\"PURPLE\"}");

        assertEquals(500, response.statusCode());
        assertTrue(response.body().contains("Color.UNKNOWN"), response.body());
        assertEquals(constant("UNKNOWN"), received.getLast());
    }

    @Test
    void sealedVariantsRoundTripUnderTheirDiscriminator() throws Exception {
        var grown = call("grow", new Class<?>[]{shape}, circle.getConstructor(int.class).newInstance(2));
        assertEquals(circle.getConstructor(int.class).newInstance(3), grown);

        var raw = post("/echo/grow", "{\"shape\":{\"type\":\"square\",\"side\":4}}");
        assertEquals(200, raw.statusCode());
        assertEquals("{\"type\":\"square\",\"side\":4}", raw.body());
    }

    @Test
    void unknownSealedVariantsReadAsTheUnknownRecordAndCannotBeWritten() throws Exception {
        var response = post("/echo/grow", "{\"shape\":{\"type\":\"hexagon\",\"sides\":6}}");

        assertEquals(500, response.statusCode());
        assertTrue(response.body().contains("Unknown(hexagon)"), response.body());
        var unknown = received.getLast();
        assertEquals("test.EchoService$Shape$Unknown", unknown.getClass().getName());
        assertEquals("hexagon", unknown.getClass().getMethod("type").invoke(unknown));
    }

    @Test
    void voidCallsReachTheImplementationAndAnswer204() throws Exception {
        assertNull(call("record", new Class<?>[]{String.class}, "landed"));
        assertTrue(recorded.contains("landed"));

        var raw = post("/echo/record", "{\"event\":\"direct\"}");
        assertEquals(204, raw.statusCode());
        assertEquals("", raw.body());
    }

    @Test
    void serverSideFailuresKeepTheirStatus() throws Exception {
        var thrown = assertThrows(InvocationTargetException.class,
            () -> call("fail", new Class<?>[0])).getCause();

        assertEquals("net.hollowcube.ipc.util.IpcException", thrown.getClass().getName());
        assertEquals(409, thrown.getClass().getMethod("status").invoke(thrown));
        assertTrue(thrown.getMessage().contains("already exists"), thrown.getMessage());
    }

    @Test
    void unknownMethodsAre404() throws Exception {
        var response = post("/echo/nope", "{}");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("no such method 'nope'"), response.body());
    }

    @Test
    void nonPostRequestsAre405() throws Exception {
        var response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(baseUrl() + "/echo/echo")).GET().build(),
            HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }


    /// The bytes are the body and never a json value, so what arrives is what was sent, however big
    /// it is: a megabyte is more than one read of the stream on either side.
    @Test
    void blobArgumentsArriveByteForByte() throws Exception {
        var bytes = bytes(1 << 20);

        assertEquals("big:" + bytes.length, call("store", new Class<?>[]{String.class, blob}, "big", blobOf(bytes)));
        assertArrayEquals(bytes, stored.get("big"));
    }

    @Test
    void blobReturnsArriveByteForByte() throws Exception {
        var bytes = bytes(1 << 20);
        call("store", new Class<?>[]{String.class, blob}, "back", blobOf(bytes));

        var answer = call("fetch", new Class<?>[]{String.class}, "back");

        assertEquals((long) bytes.length, blob.getMethod("length").invoke(answer));
        assertArrayEquals(bytes, (byte[]) blob.getMethod("readAllBytes").invoke(answer));
    }

    /// The arguments of a blob call travel in a header, which is bytes; a name written outside
    /// ascii still has to come back out as itself.
    @Test
    void blobArgumentsOutsideAsciiSurviveTheirHeader() throws Exception {
        var name = "na\u00efve \u2014 \u2705";

        assertEquals(name + ":3", call("store", new Class<?>[]{String.class, blob}, name, blobOf(new byte[]{1, 2, 3})));
        assertArrayEquals(new byte[]{1, 2, 3}, stored.get(name));
    }

    /// The wire shape itself: the body is the blob, the arguments are `x-ipc-args`, and the answer
    /// to a blob method announces its length rather than being chunked.
    @Test
    void blobCallsAreOctetStreamsWithTheirArgumentsInAHeader() throws Exception {
        var upload = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(baseUrl() + "/echo/store"))
                .header("Content-Type", "application/octet-stream")
                .header("x-ipc-args", "{\"name\":\"raw\"}")
                .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[]{7, 7, 7, 7}))
                .build(),
            HttpResponse.BodyHandlers.ofString());

        assertEquals(200, upload.statusCode());
        assertEquals("\"raw:4\"", upload.body());

        var download = post("/echo/fetch", "{\"name\":\"raw\"}");
        assertEquals(200, download.statusCode());
        assertEquals("application/octet-stream", download.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(4, download.headers().firstValueAsLong("Content-Length").orElseThrow());
    }

    @Test
    void blobFailuresKeepTheirStatus() throws Exception {
        var thrown = assertThrows(InvocationTargetException.class,
            () -> call("fetch", new Class<?>[]{String.class}, "nothing-here")).getCause();

        assertEquals("net.hollowcube.ipc.util.IpcException", thrown.getClass().getName());
        assertEquals(404, thrown.getClass().getMethod("status").invoke(thrown));
    }

    // ----- plumbing -----

    private static Compilation compile() {
        return IpcProcessorTest.compile(SERVICE);
    }

    private static Object call(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        return service.getMethod(name, parameterTypes).invoke(client, args);
    }

    private static Object newPoint(int x, int y) throws Exception {
        return point.getConstructor(int.class, int.class).newInstance(x, y);
    }

    private static Object constant(String name) throws Exception {
        return color.getField(name).get(null);
    }

    private static int x(Object p) throws Exception {
        return (int) point.getMethod("x").invoke(p);
    }

    private static int y(Object p) throws Exception {
        return (int) point.getMethod("y").invoke(p);
    }

    private static Object blobOf(byte[] bytes) throws Exception {
        return blob.getMethod("of", byte[].class).invoke(null, (Object) bytes);
    }

    /// Enough of them that neither side reads the body in one go.
    private static byte[] bytes(int length) {
        var bytes = new byte[length];
        new Random(7).nextBytes(bytes);
        return bytes;
    }

    private static RuntimeException ipcException(int status, String message) throws Exception {
        return (RuntimeException) loader.loadClass("net.hollowcube.ipc.util.IpcException")
            .getConstructor(int.class, String.class).newInstance(status, message);
    }

    private static String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static HttpResponse<String> post(String path, String body) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }

    /// Everything the in-memory compilation produced, as a loadable classpath. Gson,
    /// `com.sun.net.httpserver` and the JDK come from the parent so the generated classes share
    /// this test's copies of them; `net.hollowcube.ipc` is re-defined here from the parent's bytes
    /// so that its `Wire` resolves the generated `WireAdapters` of this compilation rather than
    /// the real module's.
    private static ClassLoader classesOf(Compilation compilation) throws IOException {
        var classes = new HashMap<String, byte[]>();
        for (var file : compilation.generatedFiles()) {
            if (file.getKind() != JavaFileObject.Kind.CLASS) continue;
            var name = file.getName();
            name = name.substring(name.indexOf("CLASS_OUTPUT/") + "CLASS_OUTPUT/".length());
            name = name.substring(0, name.length() - ".class".length()).replace('/', '.');
            try (InputStream in = file.openInputStream()) {
                classes.put(name, in.readAllBytes());
            }
        }
        var parent = IpcRoundTripTest.class.getClassLoader();
        return new ClassLoader(parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!classes.containsKey(name) && !name.startsWith("net.hollowcube.ipc.")) {
                    return super.loadClass(name, resolve);
                }
                var loaded = findLoadedClass(name);
                return loaded != null ? loaded : findClass(name);
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                var bytes = classes.get(name);
                if (bytes == null) {
                    try (var in = parent.getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (in == null) throw new ClassNotFoundException(name);
                        bytes = in.readAllBytes();
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
    }
}
