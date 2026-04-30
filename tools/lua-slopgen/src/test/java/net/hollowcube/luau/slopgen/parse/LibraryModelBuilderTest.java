package net.hollowcube.luau.slopgen.parse;

import com.google.auto.service.AutoService;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.Meta;
import net.hollowcube.luau.slopgen.Idents;
import net.hollowcube.luau.slopgen.Model;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LibraryModelBuilderTest {

    @Test
    void emptyLibrary() {
        var library = parseSingle("fixtures.LibEmpty", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            @LuaLibrary(name = "@test/empty")
            public final class LibEmpty {}
            """);
        assertEquals("@test/empty", library.moduleName());
        assertEquals(LuaLibrary.Scope.REQUIRE, library.scope());
        assertEquals(ClassName.get("fixtures", "LibEmpty"), library.sourceType());
        assertEquals(ClassName.get("fixtures", "LibEmpty$luau"), library.glueType());
        assertTrue(library.exports().isEmpty());
        assertTrue(library.staticMethods().isEmpty());
        assertTrue(library.staticProperties().isEmpty());
    }

    @Test
    void globalScopedLibrary() {
        var library = parseSingle("fixtures.LibGlobal", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaLibrary.Scope;
            @LuaLibrary(name = "myglobal", scope = Scope.GLOBAL)
            public final class LibGlobal {}
            """);
        assertEquals(LuaLibrary.Scope.GLOBAL, library.scope());
        assertEquals("myglobal", library.moduleName());
    }

    @Test
    void staticGetterAddedAsProperty() {
        var library = parseSingle("fixtures.LibStatics", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/statics")
            public final class LibStatics {
                @LuaProperty
                public static int getVersion(LuaState state) { return 1; }
            }
            """);
        assertEquals(1, library.staticProperties().size());
        var prop = library.staticProperties().get(0);
        assertEquals("version", prop.luaName());
        assertNotNull(prop.getter());
        assertNull(prop.setter());
        assertEquals("getVersion", prop.getter().javaMethodName());
    }

    @Test
    void staticNonVoidMethod() {
        var library = parseSingle("fixtures.LibStaticMethod", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/sm")
            public final class LibStaticMethod {
                @LuaMethod
                public static int build(LuaState state) { return 1; }
            }
            """);
        assertEquals(1, library.staticMethods().size());
        var m = library.staticMethods().get(0);
        assertEquals("build", m.luaName());
        assertEquals("build", m.javaMethodName());
        assertEquals(false, m.isVoid());
    }

    @Test
    void singleExportWithGetterOnly() {
        var library = parseSingle("fixtures.LibLeaf", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/leaf")
            public final class LibLeaf {
                @LuaExport
                public static final class Thing {
                    @LuaProperty
                    public int getName(LuaState state) { return 1; }
                }
            }
            """);
        assertEquals(1, library.exports().size());
        var ex = library.exports().get(0);
        assertEquals("Thing", ex.luaName());
        assertEquals(true, ex.isFinal());
        assertNull(ex.superExport());
        assertEquals(false, ex.hasSubtypes());
        assertEquals(1, ex.properties().size());
        assertEquals("name", ex.properties().get(0).luaName());
        assertNull(ex.properties().get(0).setter());
    }

    @Test
    void exportWithGetterAndSetterMergedIntoOneProperty() {
        var library = parseSingle("fixtures.LibProp", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/p")
            public final class LibProp {
                @LuaExport
                public static final class Thing {
                    @LuaProperty
                    public int getColor(LuaState state) { return 1; }
                    @LuaProperty
                    public void setColor(LuaState state) {}
                }
            }
            """);
        var ex = library.exports().get(0);
        assertEquals(1, ex.properties().size(), "getter+setter must merge into one Property");
        var prop = ex.properties().get(0);
        assertEquals("color", prop.luaName());
        assertNotNull(prop.getter());
        assertNotNull(prop.setter());
    }

    @Test
    void exportWithVoidAndNonVoidMethods() {
        var library = parseSingle("fixtures.LibMethods", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/m")
            public final class LibMethods {
                @LuaExport
                public static final class Thing {
                    @LuaMethod
                    public void poke(LuaState state) {}
                    @LuaMethod
                    public int compute(LuaState state) { return 1; }
                }
            }
            """);
        var ex = library.exports().get(0);
        assertEquals(2, ex.methods().size());
        assertEquals("poke", ex.methods().get(0).luaName());
        assertEquals(true, ex.methods().get(0).isVoid());
        assertEquals("compute", ex.methods().get(1).luaName());
        assertEquals(false, ex.methods().get(1).isVoid());
    }

    @Test
    void inheritanceChainPopulatesSuperAndHasSubtypes() {
        var library = parseSingle("fixtures.LibTree", """
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
        assertEquals(3, library.exports().size());
        var byName = new java.util.HashMap<String, com.palantir.javapoet.TypeName>();
        for (var e : library.exports()) byName.put(e.luaName(), e.javaType());

        // A has subtype B; B has subtype C; C is leaf.
        var a = findExport(library, "A");
        var b = findExport(library, "B");
        var c = findExport(library, "C");
        assertNull(a.superExport());
        assertEquals(byName.get("A"), b.superExport());
        assertEquals(byName.get("B"), c.superExport());
        assertTrue(a.hasSubtypes());
        assertTrue(b.hasSubtypes());
        assertEquals(false, c.hasSubtypes());
    }

    @Test
    void metaMethodSeparatedFromRegularMethods() {
        var library = parseSingle("fixtures.LibMeta", """
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
        var ex = library.exports().get(0);
        assertTrue(ex.methods().isEmpty());
        assertEquals(1, ex.metaMethods().size());
        assertEquals(Meta.ADD, ex.metaMethods().get(0).meta());
    }

    @Test
    void recordExportSupported() {
        var library = parseSingle("fixtures.LibRec", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            @LuaLibrary(name = "@t/r")
            public final class LibRec {
                @LuaExport
                public record Hot() {}
            }
            """);
        var ex = library.exports().get(0);
        assertEquals("Hot", ex.luaName());
        assertNull(ex.superExport(), "record's superclass (Record) is filtered out");
    }

    @Test
    void atomsReservedForExportProperties() {
        var capturing = compileWithCapture("fixtures.LibAtoms", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/atoms")
            public final class LibAtoms {
                @LuaExport
                public static final class Thing {
                    @LuaProperty public int getAlpha(LuaState s) { return 1; }
                    @LuaProperty public int getBeta(LuaState s) { return 1; }
                }
            }
            """);
        var atoms = capturing.idents.entries();
        assertEquals(2, atoms.size());
        // Source-declaration order — alpha first, beta second.
        assertEquals("alpha", atoms.get(0).luaName());
        assertEquals((short) 1, atoms.get(0).value());
        assertEquals("beta", atoms.get(1).luaName());
        assertEquals((short) 2, atoms.get(1).value());
    }

    @Test
    void instanceMethodOnLibraryClassReportsError() {
        var compilation = compile("fixtures.LibBad", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/bad")
            public final class LibBad {
                @LuaMethod
                public int notStatic(LuaState state) { return 1; }
            }
            """);
        assertThat(compilation).hadErrorContaining("Only static methods can be exported from library classes");
    }

    @Test
    void staticMethodOnExportClassReportsError() {
        var compilation = compile("fixtures.LibBad2", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/bad2")
            public final class LibBad2 {
                @LuaExport
                public static final class Thing {
                    @LuaMethod
                    public static int wrongStatic(LuaState state) { return 1; }
                }
            }
            """);
        assertThat(compilation).hadErrorContaining("Only non-static methods can be exported");
    }

    // --- helpers ---

    private static Model.Export findExport(Model.Library library, String luaName) {
        for (var e : library.exports())
            if (e.luaName().equals(luaName)) return e;
        throw new AssertionError("no export named " + luaName);
    }

    private static Model.Library parseSingle(String fqcn, String source) {
        var capturing = compileWithCapture(fqcn, source);
        assertEquals(1, capturing.captured.size(), "expected exactly one Model.Library");
        return capturing.captured.get(0);
    }

    private static Compilation compile(String fqcn, String source) {
        return Compiler.javac()
            .withProcessors(new CapturingProcessor())
            .compile(JavaFileObjects.forSourceString(fqcn, source));
    }

    private static CapturingProcessor compileWithCapture(String fqcn, String source) {
        var capturing = new CapturingProcessor();
        Compiler.javac()
            .withProcessors(capturing)
            .compile(JavaFileObjects.forSourceString(fqcn, source));
        return capturing;
    }

    @AutoService(Processor.class)
    private static final class CapturingProcessor extends AbstractProcessor {
        final List<Model.Library> captured = new ArrayList<>();
        final Idents idents = new Idents();

        @Override
        public Set<String> getSupportedAnnotationTypes() {
            return Set.of(LuaLibrary.class.getName());
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            var builder = new LibraryModelBuilder(processingEnv, idents);
            for (var el : roundEnv.getElementsAnnotatedWith(LuaLibrary.class)) {
                if (el instanceof TypeElement t) {
                    var library = builder.build(t);
                    if (library != null) captured.add(library);
                }
            }
            return true;
        }
    }
}
