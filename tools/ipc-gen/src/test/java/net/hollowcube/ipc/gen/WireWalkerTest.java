package net.hollowcube.ipc.gen;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static com.google.testing.compile.CompilationSubject.assertThat;

/// What may sit at a wire position, what may not, and what the descriptor says about the ones
/// that may.
class WireWalkerTest {

    private static final String IMPORTS = """
        package test;

        import net.hollowcube.common.util.RuntimeGson;
        import net.hollowcube.ipc.util.Ipc;
        import net.hollowcube.ipc.util.NatsMessage;
        import net.hollowcube.ipc.util.NotificationBody;
        import org.jetbrains.annotations.Nullable;
        import java.util.List;
        import java.util.Map;
        """;

    static Compilation compile(String... sources) {
        var files = new JavaFileObject[sources.length];
        for (int i = 0; i < sources.length; i++) {
            var source = IMPORTS + sources[i];
            var name = source.replaceAll("(?s).*?\\b(?:interface|record|enum|class) (\\w+).*", "$1");
            files[i] = JavaFileObjects.forSourceString("test." + name, source);
        }
        return IpcProcessorTest.compile(files);
    }

    @Test
    void rejectsRecordsThatAreNotRuntimeGson() {
        var compilation = compile("""
            @Ipc
            public interface EchoService {
                Point move(Point point);
            }
            """, """
            public record Point(int x, int y) {
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("test.Point is on the wire, so it must be @RuntimeGson");
    }

    /// The diagnostic names the whole path, because the offending type is usually a few records
    /// away from the method that put it on the wire.
    @Test
    void diagnosticsCarryTheReachingPath() {
        var compilation = compile("""
            @Ipc
            public interface EchoService {
                Outer get(String id);
            }
            """, """
            @RuntimeGson
            public record Outer(List<Inner> inners) {
            }
            """, """
            @RuntimeGson
            public record Inner(Object payload) {
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(
            "(reached via EchoService.get() returns -> Outer.inners -> Inner.payload)");
    }

    @Test
    void rejectsEnumsWithoutUnknown() {
        var compilation = compile("""
            @Ipc
            public interface EchoService {
                Color paint(Color color);
            }
            """, """
            public enum Color { RED, GREEN }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("test.Color is on the wire, so it must declare an UNKNOWN constant");
    }

    @Test
    void rejectsSqlGenTypes() {
        var compilation = compile("""
            @Ipc
            public interface EchoService {
                net.hollowcube.apiserver.db.Kind kind(String id);
            }
            """);
        var db = JavaFileObjects.forSourceString("net.hollowcube.apiserver.db.Kind", """
            package net.hollowcube.apiserver.db;

            public enum Kind { A, UNKNOWN }
            """);

        var full = IpcProcessorTest.compile(db, compilation.sourceFiles().getFirst());
        assertThat(full).failed();
        assertThat(full).hadErrorContaining("net.hollowcube.apiserver.db.Kind is a sql-gen type");
    }

    @Test
    void rejectsRawJson() {
        assertThat(compile("""
            @Ipc
            public interface EchoService {
                com.google.gson.JsonObject raw(String id);
            }
            """)).hadErrorContaining("com.google.gson.JsonObject is raw json");

        assertThat(compile("""
            @Ipc
            public interface EchoService {
                Map<String, Object> raw(String id);
            }
            """)).hadErrorContaining("java.lang.Object is raw json");
    }

    @Test
    void rejectsSealedTypesThatDoNotPermitTheirUnknownVariant() {
        var compilation = compile("""
            @Ipc
            public interface EchoService {
                Shape shape(String id);
            }
            """, """
            public sealed interface Shape permits Circle {
            }
            """, """
            @RuntimeGson
            public record Circle(int radius) implements Shape {
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("test.Shape must permit its generated unknown variant");
    }

    @Test
    void rejectsDuplicateSubjects() {
        var compilation = compile("""
            @RuntimeGson
            @NatsMessage(subject = "invite.rejected")
            public record Rejected(String id) {
            }
            """, """
            @RuntimeGson
            @NatsMessage(subject = "invite.rejected")
            public record Declined(String id) {
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("subject 'invite.rejected' is claimed by both");
    }

    /// The descriptor is what the compatibility check reads, so its shape is pinned here in full
    /// for one small wire: a service, a nullable param, a record used both ways, an enum, a sealed
    /// type, a subject and a notification key.
    @Test
    void writesTheWireDescriptor() {
        var compilation = compile("""
            @Ipc
            public interface EchoService {
                Point move(Point point, @Nullable Color color);

                void shape(Shape shape);
            }
            """, """
            @RuntimeGson
            public record Point(int x, @Nullable Integer y) {
            }
            """, """
            public enum Color { RED, GREEN, UNKNOWN }
            """, """
            public sealed interface Shape permits Circle, ShapeUnknown {
            }
            """, """
            @RuntimeGson
            public record Circle(int radius) implements Shape {
            }
            """, """
            @RuntimeGson
            @NatsMessage(subject = "point.moved")
            public record PointMoved(Point point) {
            }
            """, """
            @RuntimeGson
            @NotificationBody(type = "invite")
            public record Invite(String from) {
            }
            """);

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedFile(StandardLocation.CLASS_OUTPUT, "wire.json").contentsAsUtf8String()
            .isEqualTo("""
                {
                  "services": {
                    "echo": {
                      "interface": "test.EchoService",
                      "methods": {
                        "move": {
                          "params": [
                            {
                              "name": "point",
                              "type": "test.Point"
                            },
                            {
                              "name": "color",
                              "type": "test.Color",
                              "nullable": true
                            }
                          ],
                          "returns": {
                            "type": "test.Point"
                          }
                        },
                        "shape": {
                          "params": [
                            {
                              "name": "shape",
                              "type": "test.Shape"
                            }
                          ]
                        }
                      }
                    }
                  },
                  "types": {
                    "test.Circle": {
                      "kind": "record",
                      "used": [
                        "request"
                      ],
                      "fields": [
                        {
                          "name": "radius",
                          "type": "int"
                        }
                      ]
                    },
                    "test.Color": {
                      "kind": "enum",
                      "constants": [
                        "RED",
                        "GREEN"
                      ]
                    },
                    "test.Invite": {
                      "kind": "record",
                      "used": [
                        "body"
                      ],
                      "fields": [
                        {
                          "name": "from",
                          "type": "String"
                        }
                      ]
                    },
                    "test.Point": {
                      "kind": "record",
                      "used": [
                        "message",
                        "request",
                        "response"
                      ],
                      "fields": [
                        {
                          "name": "x",
                          "type": "int"
                        },
                        {
                          "name": "y",
                          "type": "Integer",
                          "nullable": true
                        }
                      ]
                    },
                    "test.PointMoved": {
                      "kind": "record",
                      "used": [
                        "message"
                      ],
                      "fields": [
                        {
                          "name": "point",
                          "type": "test.Point"
                        }
                      ]
                    },
                    "test.Shape": {
                      "kind": "sealed",
                      "discriminator": "type",
                      "variants": {
                        "circle": "test.Circle"
                      }
                    }
                  },
                  "subjects": {
                    "point.moved": "test.PointMoved"
                  },
                  "notifications": {
                    "invite": "test.Invite"
                  }
                }
                """);
    }
}
