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
}
