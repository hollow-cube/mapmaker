package net.hollowcube.luau.slopgen.emit;

import com.google.auto.service.AutoService;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.model.AtomTable;
import net.hollowcube.luau.slopgen.model.UserDataTagTable;
import net.hollowcube.luau.slopgen.parse.LibraryModelBuilder;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class LibraryEmitterTest {

    @Test
    void libSampleLiteralForm() throws Exception {
        // Pins LiteralResolver output for the snapshot fixture. SnapshotTest covers the symbolic
        // form (used in production); this case ensures the literal resolver still works for any
        // future caller that wants inlined atoms.
        var actual = emit("fixtures.LibSample", readResource("/snapshot/LibSample.java"));
        assertGolden("emit/lib_sample_literal.expected.java", actual);
    }

    @Test
    void emptyLibrary() throws Exception {
        var actual = emit("fixtures.LibEmpty", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            @LuaLibrary(name = "@t/empty")
            public final class LibEmpty {}
            """);
        assertGolden("emit/empty.expected.java", actual);
    }

    @Test
    void staticPropertyOnly() throws Exception {
        var actual = emit("fixtures.LibStaticProp", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/sp")
            public final class LibStaticProp {
                @LuaProperty
                public static int getVersion(LuaState state) { state.pushInteger(1); return 1; }
            }
            """);
        assertGolden("emit/static_property.expected.java", actual);
    }

    @Test
    void staticMethodNonVoid() throws Exception {
        var actual = emit("fixtures.LibStaticMethod", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/sm")
            public final class LibStaticMethod {
                @LuaMethod
                public static int build(LuaState state) { state.pushInteger(1); return 1; }
            }
            """);
        assertGolden("emit/static_method.expected.java", actual);
    }

    @Test
    void leafExportPropertiesOnly() throws Exception {
        var actual = emit("fixtures.LibLeafProps", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/leaf")
            public final class LibLeafProps {
                @LuaExport
                public static final class Thing {
                    @LuaProperty public int getColor(LuaState state) { state.pushInteger(0); return 1; }
                }
            }
            """);
        assertGolden("emit/leaf_properties.expected.java", actual);
    }

    @Test
    void exportVoidAndNonVoidMethods() throws Exception {
        var actual = emit("fixtures.LibMethods", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/m")
            public final class LibMethods {
                @LuaExport
                public static final class Thing {
                    @LuaMethod public void poke(LuaState state) {}
                    @LuaMethod public int compute(LuaState state) { state.pushInteger(0); return 1; }
                }
            }
            """);
        assertGolden("emit/export_methods.expected.java", actual);
    }

    @Test
    void exportSetterOnly() throws Exception {
        var actual = emit("fixtures.LibSetter", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/set")
            public final class LibSetter {
                @LuaExport
                public static final class Thing {
                    @LuaProperty public void setColor(LuaState state) {}
                }
            }
            """);
        assertGolden("emit/setter_only.expected.java", actual);
    }

    @Test
    void exportInheritanceChain() throws Exception {
        var actual = emit("fixtures.LibTree", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/tree")
            public final class LibTree {
                @LuaExport
                public static class A {
                    @LuaProperty public int getX(LuaState s) { return 1; }
                }
                @LuaExport
                public static class B extends A {}
                @LuaExport
                public static final class C extends B {}
            }
            """);
        assertGolden("emit/inheritance.expected.java", actual);
    }

    @Test
    void exportMetaMethod() throws Exception {
        var actual = emit("fixtures.LibMeta", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            import net.hollowcube.luau.gen.Meta;
            @LuaLibrary(name = "@t/meta")
            public final class LibMeta {
                @LuaExport
                public static final class Vec {
                    @LuaMethod(meta = Meta.ADD)
                    public int plus(LuaState state) { return 1; }
                }
            }
            """);
        assertGolden("emit/metamethod.expected.java", actual);
    }

    @Test
    void globalScope() throws Exception {
        var actual = emit("fixtures.LibGlobal", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaLibrary.Scope;
            @LuaLibrary(name = "myglobal", scope = Scope.GLOBAL)
            public final class LibGlobal {}
            """);
        assertGolden("emit/global_scope.expected.java", actual);
    }

    // ----- helpers -----

    private static String emit(String fqcn, String source) throws Exception {
        var processor = new EmittingProcessor();
        Compilation compilation = Compiler.javac()
            .withProcessors(processor)
            .compile(JavaFileObjects.forSourceString(fqcn, source));
        assertThat(compilation).succeeded();
        // The generated glue file is package + simpleName + "$luau".
        int lastDot = fqcn.lastIndexOf('.');
        var pkg = fqcn.substring(0, lastDot);
        var simple = fqcn.substring(lastDot + 1);
        var glueFqcn = pkg + "." + simple + "$luau";
        var generated = compilation.generatedSourceFile(glueFqcn)
            .orElseThrow(() -> new AssertionError("no generated source for " + glueFqcn));
        return generated.getCharContent(true).toString();
    }

    private static String readResource(String resourcePath) throws IOException {
        try (var is = LibraryEmitterTest.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Resource not found: " + resourcePath);
            return new String(is.readAllBytes());
        }
    }

    private static void assertGolden(String resourceRelativePath, String actual) throws IOException {
        Path goldenPath = Path.of("src/test/resources", resourceRelativePath);
        boolean updateRequested = Boolean.getBoolean("slopgen.update_goldens");
        if (updateRequested || !Files.exists(goldenPath)) {
            Files.createDirectories(goldenPath.getParent());
            Files.writeString(goldenPath, actual);
            if (!updateRequested)
                fail("Wrote new golden at " + goldenPath.toAbsolutePath() + " — verify and re-run");
            return;
        }
        String expected = Files.readString(goldenPath);
        assertEquals(expected, actual);
    }

    @AutoService(Processor.class)
    public static final class EmittingProcessor extends AbstractProcessor {
        @Override public Set<String> getSupportedAnnotationTypes() {
            return Set.of(LuaLibrary.class.getName());
        }

        @Override public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            var atomTable = new AtomTable();
            var tagTable = new UserDataTagTable();
            var builder = new LibraryModelBuilder(processingEnv, atomTable, tagTable);
            var emitter = new LibraryEmitter(atomTable, AtomResolver.literal(atomTable));
            for (var el : roundEnv.getElementsAnnotatedWith(LuaLibrary.class)) {
                if (el instanceof TypeElement t) {
                    var spec = builder.build(t);
                    if (spec != null) {
                        try {
                            emitter.emit(spec).writeTo(processingEnv.getFiler());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return true;
        }
    }
}
