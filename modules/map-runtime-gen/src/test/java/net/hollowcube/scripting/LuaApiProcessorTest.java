package net.hollowcube.scripting;

import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/// Direct tests of the single aggregating processor — what it writes, what it requires, and how
/// it composes across multiple `@LuaLibrary` inputs in one round.
///
/// The per-piece behavior (model build, emit shape, resolver diagnostics) is exercised by the
/// emit/parse/resolve suites; this class only verifies the processor-level contracts.
class LuaApiProcessorTest {

    @TempDir
    Path outDir;

    private Compiler compiler() {
        return Compiler.javac()
            .withProcessors(new LuaApiProcessor())
            .withOptions("-A" + LuaApiProcessor.OUTPUT_DIR_OPTION + "=" + outDir.toAbsolutePath());
    }

    @Test
    void missingOutputDirOptionFailsTheBuild() {
        // The required option contract is enforced by throwing IllegalStateException — the
        // compile-testing harness wraps AP exceptions as RuntimeException at the compile call.
        var thrown = assertThrows(RuntimeException.class, () ->
            Compiler.javac()
                .withProcessors(new LuaApiProcessor())
                .compile(JavaFileObjects.forSourceString("fixtures.LibX", """
                    package fixtures;
                    import net.hollowcube.scripting.gen.LuaLibrary;
                    @LuaLibrary(name = "@t/x") public final class LibX {}
                    """)));
        // Walk the cause chain to find the originating IllegalStateException — the harness
        // wraps it once or twice depending on JDK version.
        Throwable t = thrown;
        while (t != null && !(t instanceof IllegalStateException)) t = t.getCause();
        assertNotNull(t, "expected the failure root cause to be IllegalStateException; got " + thrown);
        assertTrue(t.getMessage().contains(LuaApiProcessor.OUTPUT_DIR_OPTION),
            "exception message should name the missing option `" + LuaApiProcessor.OUTPUT_DIR_OPTION
            + "`. Got: " + t.getMessage());
    }

    @Test
    void multiLibraryRoundProducesPartitionedEngineApi() throws Exception {
        // Two libraries in one round: one REQUIRE-scope, one GLOBAL-scope. Verifies that the
        // aggregate JSON puts them under `libraries` / `globals` respectively and that the .luau
        // bundle includes both the per-module file and global.d.luau.
        var compilation = compiler().compile(
            JavaFileObjects.forSourceString("fixtures.LibReq", """
                package fixtures;
                import net.hollowcube.luau.LuaState;
                import net.hollowcube.scripting.gen.LuaLibrary;
                import net.hollowcube.scripting.gen.LuaProperty;
                @LuaLibrary(name = "@t/req")
                public final class LibReq {
                    /// @luaReturn number
                    @LuaProperty public static int getN(LuaState s) { s.pushInteger(1); return 1; }
                }
                """),
            JavaFileObjects.forSourceString("fixtures.LibGlob", """
                package fixtures;
                import net.hollowcube.luau.LuaState;
                import net.hollowcube.scripting.gen.LuaLibrary;
                import net.hollowcube.scripting.gen.LuaLibrary.Scope;
                import net.hollowcube.scripting.gen.LuaProperty;
                @LuaLibrary(name = "myglobal", scope = Scope.GLOBAL)
                public final class LibGlob {
                    /// @luaReturn string
                    @LuaProperty public static int getTag(LuaState s) { s.pushString("g"); return 1; }
                }
                """));
        assertThat(compilation).succeeded();

        // Java glue for both — proves per-library emission completes for the full set.
        assertTrue(compilation.generatedSourceFile("fixtures.LibReq$luau").isPresent());
        assertTrue(compilation.generatedSourceFile("fixtures.LibGlob$luau").isPresent());

        // engine-api.json partitions by scope.
        var engineApi = outDir.resolve("engine-api.json");
        assertTrue(Files.exists(engineApi), "engine-api.json should exist at " + engineApi);
        var json = Files.readString(engineApi);
        // Cheap structural assertions — the SchemaJson shape is pinned by SnapshotTest.
        assertTrue(json.contains("\"libraries\""), "expected `libraries` key");
        assertTrue(json.contains("\"globals\""), "expected `globals` key");
        assertTrue(json.contains("\"@t/req\""), "REQUIRE-scope library should appear under libraries");
        assertTrue(json.contains("\"myglobal\""), "GLOBAL-scope library should appear in globals");

        // Type bundle: per-module require + global ambient.
        assertTrue(Files.exists(outDir.resolve("types/@t/req.luau")),
            "expected REQUIRE-scope .luau module to be emitted");
        assertTrue(Files.exists(outDir.resolve("types/global.d.luau")),
            "expected global.d.luau because at least one library is GLOBAL-scoped");
    }

    @Test
    void crossLibraryReferenceResolvesAndEmitsRequire() throws Exception {
        // Library A defines an export; library B's accessor returns A's export by fully-qualified
        // module ref. The aggregate AP must resolve the cross-module symbol (no diagnostic) and
        // the emitted .luau module for B must include `require(...)` for A.
        var compilation = compiler().compile(
            JavaFileObjects.forSourceString("fixtures.LibA", """
                package fixtures;
                import net.hollowcube.luau.LuaState;
                import net.hollowcube.scripting.gen.LuaExport;
                import net.hollowcube.scripting.gen.LuaLibrary;
                import net.hollowcube.scripting.gen.LuaProperty;
                @LuaLibrary(name = "@t/a")
                public final class LibA {
                    @LuaExport public static final class Thing {
                        /// @luaReturn string
                        @LuaProperty public int getId(LuaState s) { s.pushString("x"); return 1; }
                    }
                }
                """),
            JavaFileObjects.forSourceString("fixtures.LibB", """
                package fixtures;
                import net.hollowcube.luau.LuaState;
                import net.hollowcube.scripting.gen.LuaLibrary;
                import net.hollowcube.scripting.gen.LuaMethod;
                @LuaLibrary(name = "@t/b")
                public final class LibB {
                    /// @luaReturn @t/a.Thing
                    @LuaMethod public static int find(LuaState s) { return 1; }
                }
                """));
        assertThat(compilation).succeeded();
        // The resolver should NOT have produced an "unresolved cross-library type" warning.
        for (var d : compilation.diagnostics()) {
            var msg = d.getMessage(null);
            assertFalse(msg != null && msg.contains("unresolved cross-library type"),
                "resolver flagged a cross-library type that should resolve: " + msg);
        }
        var libB = Files.readString(outDir.resolve("types/@t/b.luau"));
        assertTrue(libB.contains("require(\"./a\")"),
            "LibB module should require LibA via a relative path. Got:\n" + libB);
        assertTrue(libB.contains(".Thing"),
            "LibB module should reference the foreign type by alias.Thing. Got:\n" + libB);
    }

    @Test
    void writableMetaTypeExpandedBeforeOutput() throws Exception {
        // A library with one export `Thing` that has a writable `name` and a writable `count`,
        // plus a static method whose param uses `$Writable<Thing>`. After processing:
        //   - no '$' appears anywhere in engine-api.json or in the .luau bundle
        //   - the .luau bundle types `set` as `{name: string?, count: number?}`
        var compilation = compiler().compile(
            JavaFileObjects.forSourceString("fixtures.LibMeta", """
                package fixtures;
                import net.hollowcube.luau.LuaState;
                import net.hollowcube.scripting.gen.LuaExport;
                import net.hollowcube.scripting.gen.LuaLibrary;
                import net.hollowcube.scripting.gen.LuaMethod;
                import net.hollowcube.scripting.gen.LuaProperty;
                @LuaLibrary(name = "@t/meta")
                public final class LibMeta {
                    @LuaExport public static final class Thing {
                        /// @luaReturn string
                        @LuaProperty public int getName(LuaState s) { return 1; }
                        /// @luaParam value string
                        @LuaProperty public void setName(LuaState s) {}
                        /// @luaReturn number
                        @LuaProperty public int getCount(LuaState s) { return 1; }
                        /// @luaParam value number
                        @LuaProperty public void setCount(LuaState s) {}
                    }
                    /// @luaParam props $Writable<Thing>
                    @LuaMethod public static void set(LuaState s) {}
                }
                """));
        assertThat(compilation).succeeded();

        var json = Files.readString(outDir.resolve("engine-api.json"));
        assertFalse(json.contains("$Writable"),
            "engine-api.json should not contain the unexpanded $Writable marker:\n" + json);
        // The expanded shape should appear: a Table with name/count fields.
        assertTrue(json.contains("\"name\""), () -> "expected 'name' in JSON:\n" + json);
        assertTrue(json.contains("\"count\""), () -> "expected 'count' in JSON:\n" + json);

        var luau = Files.readString(outDir.resolve("types/@t/meta.luau"));
        assertFalse(luau.contains("$"),
            ".luau bundle should not contain any '$' character (meta-type residue):\n" + luau);
        // Both writable fields should be present as optional record fields.
        assertTrue(luau.contains("name: string?"),
            () -> "expected `name: string?` in .luau output:\n" + luau);
        assertTrue(luau.contains("count: number?"),
            () -> "expected `count: number?` in .luau output:\n" + luau);
    }
}
