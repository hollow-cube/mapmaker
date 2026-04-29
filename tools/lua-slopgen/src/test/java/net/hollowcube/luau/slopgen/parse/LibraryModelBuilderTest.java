package net.hollowcube.luau.slopgen.parse;

import com.google.auto.service.AutoService;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.Meta;
import net.hollowcube.luau.slopgen.model.AtomTable;
import net.hollowcube.luau.slopgen.model.LibrarySpec;
import net.hollowcube.luau.slopgen.model.UserDataTagTable;
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
        var spec = parseSingle("fixtures.LibEmpty", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            @LuaLibrary(name = "@test/empty")
            public final class LibEmpty {}
            """);
        assertEquals("@test/empty", spec.moduleName());
        assertEquals(LuaLibrary.Scope.REQUIRE, spec.scope());
        assertEquals(ClassName.get("fixtures", "LibEmpty"), spec.sourceType());
        assertEquals(ClassName.get("fixtures", "LibEmpty$luau"), spec.glueType());
        assertTrue(spec.exports().isEmpty());
        assertTrue(spec.staticMethods().isEmpty());
        assertTrue(spec.staticProperties().isEmpty());
    }

    @Test
    void globalScopedLibrary() {
        var spec = parseSingle("fixtures.LibGlobal", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaLibrary.Scope;
            @LuaLibrary(name = "myglobal", scope = Scope.GLOBAL)
            public final class LibGlobal {}
            """);
        assertEquals(LuaLibrary.Scope.GLOBAL, spec.scope());
        assertEquals("myglobal", spec.moduleName());
    }

    @Test
    void staticGetterAddedAsProperty() {
        var spec = parseSingle("fixtures.LibStatics", """
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
        assertEquals(1, spec.staticProperties().size());
        var prop = spec.staticProperties().get(0);
        assertEquals("version", prop.luaName());
        assertNotNull(prop.getter());
        assertNull(prop.setter());
        assertEquals("getVersion", prop.getter().javaMethodName());
    }

    @Test
    void staticNonVoidMethod() {
        var spec = parseSingle("fixtures.LibStaticMethod", """
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
        assertEquals(1, spec.staticMethods().size());
        var m = spec.staticMethods().get(0);
        assertEquals("build", m.luaName());
        assertEquals("build", m.javaMethodName());
        assertEquals(false, m.isVoid());
    }

    @Test
    void singleExportWithGetterOnly() {
        var spec = parseSingle("fixtures.LibLeaf", """
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
        assertEquals(1, spec.exports().size());
        var ex = spec.exports().get(0);
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
        var spec = parseSingle("fixtures.LibProp", """
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
        var ex = spec.exports().get(0);
        assertEquals(1, ex.properties().size(), "getter+setter must merge into one PropertySpec");
        var prop = ex.properties().get(0);
        assertEquals("color", prop.luaName());
        assertNotNull(prop.getter());
        assertNotNull(prop.setter());
    }

    @Test
    void exportWithVoidAndNonVoidMethods() {
        var spec = parseSingle("fixtures.LibMethods", """
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
        var ex = spec.exports().get(0);
        assertEquals(2, ex.methods().size());
        assertEquals("poke", ex.methods().get(0).luaName());
        assertEquals(true, ex.methods().get(0).isVoid());
        assertEquals("compute", ex.methods().get(1).luaName());
        assertEquals(false, ex.methods().get(1).isVoid());
    }

    @Test
    void inheritanceChainPopulatesSuperAndHasSubtypes() {
        var spec = parseSingle("fixtures.LibTree", """
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
        assertEquals(3, spec.exports().size());
        var byName = new java.util.HashMap<String, com.palantir.javapoet.TypeName>();
        for (var e : spec.exports()) byName.put(e.luaName(), e.javaType());

        // A has subtype B; B has subtype C; C is leaf.
        var a = findExport(spec, "A");
        var b = findExport(spec, "B");
        var c = findExport(spec, "C");
        assertNull(a.superExport());
        assertEquals(byName.get("A"), b.superExport());
        assertEquals(byName.get("B"), c.superExport());
        assertTrue(a.hasSubtypes());
        assertTrue(b.hasSubtypes());
        assertEquals(false, c.hasSubtypes());
    }

    @Test
    void metaMethodSeparatedFromRegularMethods() {
        var spec = parseSingle("fixtures.LibMeta", """
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
        var ex = spec.exports().get(0);
        assertTrue(ex.methods().isEmpty());
        assertEquals(1, ex.metaMethods().size());
        assertEquals(Meta.ADD, ex.metaMethods().get(0).meta());
    }

    @Test
    void recordExportSupported() {
        var spec = parseSingle("fixtures.LibRec", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            @LuaLibrary(name = "@t/r")
            public final class LibRec {
                @LuaExport
                public record Hot() {}
            }
            """);
        var ex = spec.exports().get(0);
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
        var atoms = capturing.atomTable.entries();
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

    private static com.palantir.javapoet.TypeName typeName(LibrarySpec spec, String exportLuaName) {
        return findExport(spec, exportLuaName).javaType();
    }

    private static net.hollowcube.luau.slopgen.model.ExportSpec findExport(LibrarySpec spec, String luaName) {
        for (var e : spec.exports())
            if (e.luaName().equals(luaName)) return e;
        throw new AssertionError("no export named " + luaName);
    }

    private static LibrarySpec parseSingle(String fqcn, String source) {
        var capturing = compileWithCapture(fqcn, source);
        assertEquals(1, capturing.captured.size(), "expected exactly one LibrarySpec");
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
        final List<LibrarySpec> captured = new ArrayList<>();
        final AtomTable atomTable = new AtomTable();
        final UserDataTagTable userDataTagTable = new UserDataTagTable();

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
            var builder = new LibraryModelBuilder(processingEnv, atomTable, userDataTagTable);
            for (var el : roundEnv.getElementsAnnotatedWith(LuaLibrary.class)) {
                if (el instanceof TypeElement t) {
                    var spec = builder.build(t);
                    if (spec != null) captured.add(spec);
                }
            }
            return true;
        }
    }
}
