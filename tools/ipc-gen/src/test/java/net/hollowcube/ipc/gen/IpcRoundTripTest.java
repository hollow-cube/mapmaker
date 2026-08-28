package net.hollowcube.ipc.gen;

import com.google.gson.Gson;
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
import java.util.concurrent.Executors;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/// Compiles a service, loads the two generated classes, and drives one against the other over a
/// real [HttpServer].
///
/// The source assertions in [IpcProcessorTest] say what the emitters wrote; this says the two
/// halves agree — which is the only property that matters and the one a string route cannot give
/// you. Serialization is where a generated client and a hand-written server drift, so it is checked
/// end to end rather than per side.
class IpcRoundTripTest {

    private static final Gson GSON = new Gson();

    private static final JavaFileObject SERVICE = JavaFileObjects.forSourceString("test.EchoService", """
        package test;

        import net.hollowcube.ipc.util.Ipc;
        import java.util.List;
        import java.util.Map;

        @Ipc
        public interface EchoService {

            String echo(String message, int count);

            List<String> split(String value, String separator);

            Map<String, List<Point>> group(List<Point> points);

            Point move(Point point, int dx);

            void record(String event);

            String fail();

            record Point(int x, int y) {
            }
        }
        """);

    private static ClassLoader loader;
    private static Class<?> service;
    private static Class<?> point;
    private static HttpServer server;
    private static Object client;
    private static List<String> recorded;

    @BeforeAll
    static void startServer() throws Exception {
        var compilation = compile();
        assertThat(compilation).succeededWithoutWarnings();

        loader = classesOf(compilation);
        service = loader.loadClass("test.EchoService");
        point = loader.loadClass("test.EchoService$Point");
        recorded = new ArrayList<>();

        var impl = Proxy.newProxyInstance(loader, new Class<?>[]{service}, (proxy, method, args) ->
            switch (method.getName()) {
                case "echo" -> args[0] + ":" + args[1];
                case "split" -> List.of(((String) args[0]).split((String) args[1]));
                case "group" -> Map.of("all", (List<?>) args[0]);
                case "move" -> point.getConstructor(int.class, int.class)
                    .newInstance(x(args[0]) + (int) args[1], y(args[0]));
                case "record" -> {
                    recorded.add((String) args[0]);
                    yield null;
                }
                case "fail" -> throw (RuntimeException) loader.loadClass("net.hollowcube.ipc.util.IpcException")
                    .getConstructor(int.class, String.class).newInstance(409, "already exists");
                default -> throw new UnsupportedOperationException(method.getName());
            });

        var serverClass = loader.loadClass("test.EchoServer");
        var handler = (HttpHandler) serverClass.getConstructor(service, Gson.class).newInstance(impl, GSON);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext((String) serverClass.getField("PATH").get(null), handler);
        server.start();

        var clientClass = loader.loadClass("test.EchoClient");
        client = clientClass.getConstructor(HttpClient.class, Gson.class, String.class)
            .newInstance(HttpClient.newHttpClient(), GSON, baseUrl() + "/");
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

    private static int x(Object p) throws Exception {
        return (int) point.getMethod("x").invoke(p);
    }

    private static int y(Object p) throws Exception {
        return (int) point.getMethod("y").invoke(p);
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
    /// this test's copies of them.
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
        return new ClassLoader(IpcRoundTripTest.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                var bytes = classes.get(name);
                if (bytes == null) throw new ClassNotFoundException(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
    }
}
