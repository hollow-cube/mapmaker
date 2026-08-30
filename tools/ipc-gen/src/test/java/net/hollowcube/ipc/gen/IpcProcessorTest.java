package net.hollowcube.ipc.gen;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;

/// What the processor accepts, what it refuses, and what the two emitted files say.
class IpcProcessorTest {

    static Compilation compile(JavaFileObject... sources) {
        return Compiler.javac().withProcessors(new IpcProcessor()).compile(sources);
    }

    static JavaFileObject service(String body) {
        return JavaFileObjects.forSourceString("test.EchoService", """
            package test;

            import net.hollowcube.common.util.RuntimeGson;
            import net.hollowcube.ipc.Blob;
            import net.hollowcube.ipc.util.Ipc;
            import org.jetbrains.annotations.Nullable;
            import java.util.List;
            import java.util.Map;

            @Ipc
            public interface EchoService {
            %s
            }
            """.formatted(body));
    }

    @Test
    void emitsBothHalvesInTheServicePackage() {
        var compilation = compile(service("    String echo(String message, int count);"));

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("test.EchoClient");
        assertThat(compilation).generatedSourceFile("test.EchoServer");
    }

    @Test
    void clientPostsTheMethodNameUnderTheInterfacePath() {
        var compilation = compile(service("    String echo(String message, int count);"));
        assertThat(compilation).succeededWithoutWarnings();

        var client = assertThat(compilation).generatedSourceFile("test.EchoClient").contentsAsUtf8String();
        client.contains("PATH = \"/echo\"");
        client.contains("private static final Gson GSON = Wire.gson()");
        client.contains("ipcRequest.add(\"message\", GSON.toJsonTree(message, String.class))");
        client.contains("ipcRequest.add(\"count\", GSON.toJsonTree(count, Integer.class))");
        client.contains("return GSON.fromJson(call(\"echo\", ipcRequest), String.class)");
        client.contains("String url = baseUrl + PATH + \"/\" + method");
    }

    @Test
    void serverRoutesOnTheTrailingSegment() {
        var compilation = compile(service("    String echo(String message, int count);"));
        assertThat(compilation).succeededWithoutWarnings();

        var server = assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String();
        server.contains("PATH = \"/echo\"");
        server.contains("String ipcMethod = ipcPath.substring(ipcPath.lastIndexOf('/') + 1)");
        server.contains("case \"echo\" ->");
        server.contains("String message = GSON.fromJson(ipcArgument, String.class)");
        server.contains("int count = GSON.fromJson(ipcArgument, Integer.class)");
        server.contains("ipcResponse = GSON.toJsonTree(impl.echo(message, count), String.class)");
    }

    /// A parameter the method does not mark `@Nullable` is one the implementation was promised;
    /// the server keeps that promise with a 400 rather than handing over a null.
    @Test
    void serverRejectsMissingNonNullParametersAndPassesNullableOnes() {
        var compilation = compile(service("    String echo(String message, @Nullable String suffix);"));
        assertThat(compilation).succeededWithoutWarnings();

        var server = assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String();
        server.contains("respondError(exchange, span, 400, \"missing parameter 'message'\")");
        server.doesNotContain("missing parameter 'suffix'");
        server.contains("String suffix = GSON.fromJson(ipcArgument, String.class)");
    }

    /// The client says what it is on every call, which is how the server side learns how old its
    /// callers are before `wire-baseline` moves.
    @Test
    void clientSendsItsVersion() {
        var compilation = compile(service("    String echo(String message);"));
        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation).generatedSourceFile("test.EchoClient").contentsAsUtf8String()
            .contains(".header(Wire.CLIENT_HEADER, Wire.clientVersion())");
    }

    /// A generic type has to survive erasure to be read back, and `TypeToken.getParameterized` is
    /// the reflection-free way to say so — the shape native image does not have to be told about.
    @Test
    void genericTypesBecomeHoistedTypeTokenConstants() {
        var compilation = compile(service("    List<String> split(String value);"));
        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation).generatedSourceFile("test.EchoClient").contentsAsUtf8String()
            .contains("TYPE_split = TypeToken.getParameterized(List.class, String.class).getType()");
        assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String()
            .contains("TYPE_split = TypeToken.getParameterized(List.class, String.class).getType()");
    }

    @Test
    void voidMethodsAnswerWithNoBody() {
        var compilation = compile(service("    void noop(String value);"));
        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation).generatedSourceFile("test.EchoClient").contentsAsUtf8String()
            .contains("call(\"noop\", ipcRequest);");
        assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String()
            .contains("ipcResponse = null");
    }

    /// Inherited methods are implemented too — dropping them would produce a class that does not
    /// compile instead of a diagnostic.
    @Test
    void inheritedMethodsAreImplemented() {
        var base = JavaFileObjects.forSourceString("test.Base", """
            package test;

            public interface Base {
                String inherited(String value);
            }
            """);
        var compilation = compile(base, JavaFileObjects.forSourceString("test.EchoService", """
            package test;

            import net.hollowcube.ipc.util.Ipc;

            @Ipc
            public interface EchoService extends Base {
                String echo(String message);
            }
            """));

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String()
            .contains("case \"inherited\" ->");
    }

    /// A `default` method is implemented on both sides already, so it is not a route.
    @Test
    void defaultMethodsAreNotRoutes() {
        var compilation = compile(service("""
                String echo(String message);

                default String twice(String message) {
                    return echo(message) + echo(message);
                }
            """));

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String()
            .doesNotContain("case \"twice\"");
    }

    @Test
    void rejectsAnnotatedClass() {
        var compilation = compile(JavaFileObjects.forSourceString("test.EchoService", """
            package test;

            import net.hollowcube.ipc.util.Ipc;

            @Ipc
            public class EchoService {
            }
            """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Ipc must be an interface");
    }

    @Test
    void rejectsGenericInterface() {
        var compilation = compile(JavaFileObjects.forSourceString("test.EchoService", """
            package test;

            import net.hollowcube.ipc.util.Ipc;

            @Ipc
            public interface EchoService<T> {
                T echo(T message);
            }
            """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cannot be generic");
    }

    /// Routes are what the two halves agree on, so the rule that produces them is worth pinning:
    /// the interface name without a trailing `Service`, then every name in kebab case.
    @Test
    void routesAreKebabCaseWithoutTheServiceSuffix() {
        var compilation = compile(JavaFileObjects.forSourceString("test.HeadDatabaseService", """
            package test;

            import net.hollowcube.ipc.util.Ipc;

            @Ipc
            public interface HeadDatabaseService {
                String getHeadsInCategory(String category);

                String getHDBEntry(String id);
            }
            """));
        assertThat(compilation).succeededWithoutWarnings();

        var client = assertThat(compilation).generatedSourceFile("test.HeadDatabaseClient").contentsAsUtf8String();
        client.contains("PATH = \"/head-database\"");
        client.contains("call(\"get-heads-in-category\", ipcRequest)");
        client.contains("call(\"get-hdb-entry\", ipcRequest)");

        var server = assertThat(compilation).generatedSourceFile("test.HeadDatabaseServer").contentsAsUtf8String();
        server.contains("PATH = \"/head-database\"");
        server.contains("case \"get-heads-in-category\" ->");
        server.contains("case \"get-hdb-entry\" ->");
    }

    /// Both halves trace, and both name their spans after the same service, which is what makes a
    /// call and the work it caused one trace rather than two.
    @Test
    void bothHalvesOpenASpanNamedAfterTheService() {
        var compilation = compile(service("    String echo(String message);"));
        assertThat(compilation).succeededWithoutWarnings();

        var client = assertThat(compilation).generatedSourceFile("test.EchoClient").contentsAsUtf8String();
        client.contains("this.tracing = new IpcTracing(otel, \"echo\")");
        client.contains("try (IpcSpan span = tracing.client(method, httpRequest, url))");
        client.contains("span.status(response.statusCode())");

        var server = assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String();
        server.contains("this.tracing = new IpcTracing(otel, \"echo\")");
        server.contains("try (IpcSpan span = tracing.server(ipcMethod, exchange))");
        server.contains("span.failed(e)");
    }

    /// The method name is the route, so two methods of one name are two methods at one address.
    @Test
    void rejectsOverloads() {
        var compilation = compile(service("""
                String echo(String message);

                String echo(String message, int count);
            """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cannot be overloaded");
    }

    @Test
    void rejectsGenericMethod() {
        var compilation = compile(service("    <T> T echo(String message);"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("ipc methods cannot be generic");
    }

    @Test
    void rejectsWildcardParameter() {
        var compilation = compile(service("    String echo(List<? extends CharSequence> messages);"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cannot serialize");
    }

    /// A blob is the request body, so the arguments it would have shared that body with move into a
    /// header. Everything else about the call — the route, the version, the span — is unchanged.
    @Test
    void blobParametersAreTheRequestBodyAndPushTheArgumentsIntoAHeader() {
        var compilation = compile(service("    String store(String name, Blob body);"));
        assertThat(compilation).succeededWithoutWarnings();

        var client = assertThat(compilation).generatedSourceFile("test.EchoClient").contentsAsUtf8String();
        client.contains("ipcRequest.add(\"name\", GSON.toJsonTree(name, String.class))");
        client.contains("return GSON.fromJson(json(callStream(\"store\", ipcRequest, body)), String.class)");
        client.contains(".header(Wire.ARGS_HEADER, Wire.args(request))");
        client.contains("HttpRequest.BodyPublishers.ofInputStream(body::stream)");
        client.contains("HttpRequest.BodyPublishers.fromPublisher(ipcBody, body.length())");
        // The one method is a blob call, so the json plumbing it never reaches is not written.
        client.doesNotContain("private JsonElement call(");

        var server = assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String();
        server.contains("BLOB_REQUESTS = Set.of(\"store\")");
        server.contains("if (BLOB_REQUESTS.contains(ipcMethod))");
        server.contains("String ipcArgs = exchange.getRequestHeaders().getFirst(Wire.ARGS_HEADER)");
        server.contains("Blob body = new Blob(length(exchange), exchange.getRequestBody())");
        server.contains("ipcResponse = GSON.toJsonTree(impl.store(name, body), String.class)");
    }

    /// A blob answer is the response body, written as it is read and never held.
    @Test
    void blobReturnsAreTheResponseBody() {
        var compilation = compile(service("    Blob fetch(String name);"));
        assertThat(compilation).succeededWithoutWarnings();

        var client = assertThat(compilation).generatedSourceFile("test.EchoClient").contentsAsUtf8String();
        client.contains("return blob(callStream(\"fetch\", ipcRequest, null))");
        client.contains("response.headers().firstValueAsLong(\"content-length\").orElse(-1)");
        // Nothing about the request changed, so it is the json body every other call sends.
        client.contains("callStream(\"fetch\", ipcRequest, null)");
        client.contains("HttpRequest.BodyPublishers.ofString(request.toString(), StandardCharsets.UTF_8)");

        var server = assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String();
        server.contains("respondBlob(exchange, span, impl.fetch(name))");
        server.contains("exchange.getResponseHeaders().set(\"Content-Type\", \"application/octet-stream\")");
        server.doesNotContain("BLOB_REQUESTS");
    }

    /// The parameters are read in two passes — json arguments, then the body — and the call is
    /// still made in the order the interface declares.
    @Test
    void blobParametersKeepTheirPlaceInTheCall() {
        var compilation = compile(service("    String store(Blob body, String name);"));
        assertThat(compilation).succeededWithoutWarnings();

        assertThat(compilation).generatedSourceFile("test.EchoServer").contentsAsUtf8String()
            .contains("impl.store(body, name)");
    }

    /// A service with both kinds of method keeps the json plumbing for the methods that use it.
    @Test
    void aServiceWithOneBlobMethodStillPostsJsonForTheRest() {
        var compilation = compile(service("""
                String echo(String message);

                Blob fetch(String name);
            """));
        assertThat(compilation).succeededWithoutWarnings();

        var client = assertThat(compilation).generatedSourceFile("test.EchoClient").contentsAsUtf8String();
        client.contains("return GSON.fromJson(call(\"echo\", ipcRequest), String.class)");
        client.contains("return blob(callStream(\"fetch\", ipcRequest, null))");
    }

    @Test
    void rejectsTwoBlobParameters() {
        var compilation = compile(service("    String store(Blob first, Blob second);"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("at most one blob");
    }

    @Test
    void rejectsANullableBlob() {
        var compilation = compile(service("    String store(@Nullable Blob body);"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cannot be null");
    }

    /// A blob is a whole body, so there is nowhere in a json value to put one.
    @Test
    void rejectsABlobInsideARecord() {
        var compilation = compile(service("""
                Upload upload(Upload upload);

                @RuntimeGson
                record Upload(String name, Blob body) {
                }
            """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("cannot be a field of one");
    }
}
