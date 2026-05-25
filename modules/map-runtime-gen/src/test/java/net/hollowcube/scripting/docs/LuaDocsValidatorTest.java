package net.hollowcube.scripting.docs;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import net.hollowcube.scripting.LuaApiProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;

/// End-to-end checks that validation rules attribute errors to the right element by running the
/// real processor over inline source. The validator implementation is exercised indirectly via
/// the `Messager` errors it produces.
class LuaDocsValidatorTest {

    @TempDir
    Path outDir;

    @Test
    void nonVoidMethodWithoutLuaReturnFails() {
        var compilation = compile("fixtures.LibBadM", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaMethod;
            @LuaLibrary(name = "@t/badm")
            public final class LibBadM {
                @LuaMethod public static int oops(LuaState s) { s.pushInteger(1); return 1; }
            }
            """);
        // Non-void without @luaReturn would emit a Luau signature returning nothing while the
        // Java implementation pushes a value — always wrong, hard-error.
        assertThat(compilation).hadErrorContaining("must declare at least one @luaReturn");
    }

    @Test
    void voidMethodWithLuaReturnFails() {
        var compilation = compile("fixtures.LibVoid", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaMethod;
            @LuaLibrary(name = "@t/void")
            public final class LibVoid {
                /// @luaReturn nil
                @LuaMethod public static void noop(LuaState s) {}
            }
            """);
        assertThat(compilation).hadWarningContaining("Void @LuaMethod must not declare @luaReturn");
    }

    @Test
    void multipleReturnsAccepted() {
        var compilation = compile("fixtures.LibMulti", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaMethod;
            @LuaLibrary(name = "@t/multi")
            public final class LibMulti {
                /// @luaReturn number
                /// @luaReturn string
                @LuaMethod public static int pair(LuaState s) { return 2; }
            }
            """);
        assertThat(compilation).succeeded();
    }

    @Test
    void getterMissingLuaReturnFails() {
        var compilation = compile("fixtures.LibG", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaProperty;
            @LuaLibrary(name = "@t/g")
            public final class LibG {
                @LuaProperty public static int getX(LuaState s) { return 1; }
            }
            """);
        // Missing @luaReturn emits a `nil`-typed property — silently breaks every reader.
        assertThat(compilation).hadErrorContaining(
            "@LuaProperty getter must declare exactly one @luaReturn");
    }

    @Test
    void getterWithLuaParamFails() {
        var compilation = compile("fixtures.LibGp", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaProperty;
            @LuaLibrary(name = "@t/gp")
            public final class LibGp {
                /// @luaReturn number
                /// @luaParam wrong number
                @LuaProperty public static int getX(LuaState s) { return 1; }
            }
            """);
        assertThat(compilation).hadWarningContaining("getter must not declare @luaParam");
    }

    @Test
    void setterMissingLuaParamFails() {
        var compilation = compile("fixtures.LibS", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaProperty;
            @LuaLibrary(name = "@t/s")
            public final class LibS {
                @LuaProperty public static void setX(LuaState s) {}
            }
            """);
        // Missing @luaParam emits a setter accepting `nil` only — never intended.
        assertThat(compilation).hadErrorContaining(
            "@LuaProperty setter must declare exactly one @luaParam");
    }

    @Test
    void setterWithLuaReturnFails() {
        var compilation = compile("fixtures.LibSr", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaProperty;
            @LuaLibrary(name = "@t/sr")
            public final class LibSr {
                /// @luaParam value number
                /// @luaReturn number
                @LuaProperty public static void setX(LuaState s) {}
            }
            """);
        assertThat(compilation).hadWarningContaining("setter must not declare @luaReturn");
    }

    @Test
    void luaTagOnLibraryClassFails() {
        var compilation = compile("fixtures.LibBadDoc", """
            package fixtures;
            import net.hollowcube.scripting.gen.LuaLibrary;
            /// @luaParam wrong number
            @LuaLibrary(name = "@t/baddoc")
            public final class LibBadDoc {}
            """);
        assertThat(compilation).hadWarningContaining("not valid on a library");
    }

    @Test
    void luaTagOnExportClassFails() {
        var compilation = compile("fixtures.LibExDoc", """
            package fixtures;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaExport;
            @LuaLibrary(name = "@t/exdoc")
            public final class LibExDoc {
                /// @luaReturn number
                @LuaExport public static final class Thing {}
            }
            """);
        assertThat(compilation).hadWarningContaining("not valid on a @LuaExport class");
    }

    @Test
    void malformedLuaTagFails() {
        var compilation = compile("fixtures.LibBadTag", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaMethod;
            @LuaLibrary(name = "@t/badtag")
            public final class LibBadTag {
                /// @luaParam
                /// @luaReturn number
                @LuaMethod public static int oops(LuaState s) { return 1; }
            }
            """);
        // Malformed @lua tag is dropped silently, so codegen emits a signature missing the
        // param/return entirely. Hard-error so authors can't ship the typo.
        assertThat(compilation).hadErrorContaining("Malformed slopgen tag");
    }

    @Test
    void methodMissingParamTypeFails() {
        // `@luaParam x` with no type body is rejected by the regex layer as a malformed tag.
        // Asserted separately from the param-name-only case above because this is the more
        // common drift scenario (added a Java param, forgot to give the @luaParam a type).
        var compilation = compile("fixtures.LibMissingParam", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.scripting.gen.LuaLibrary;
            import net.hollowcube.scripting.gen.LuaMethod;
            @LuaLibrary(name = "@t/mp")
            public final class LibMissingParam {
                /// @luaParam x
                /// @luaReturn number
                @LuaMethod public static int oops(LuaState s) { return 1; }
            }
            """);
        assertThat(compilation).hadErrorContaining("Malformed slopgen tag");
    }

    private Compilation compile(String fqcn, String source) {
        return Compiler.javac()
            .withProcessors(new LuaApiProcessor())
            .withOptions("-A" + LuaApiProcessor.OUTPUT_DIR_OPTION + "=" + outDir.toAbsolutePath())
            .compile(JavaFileObjects.forSourceString(fqcn, source));
    }
}
