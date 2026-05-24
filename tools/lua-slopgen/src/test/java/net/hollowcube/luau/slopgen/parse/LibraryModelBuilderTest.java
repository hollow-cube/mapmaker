package net.hollowcube.luau.slopgen.parse;

import com.google.auto.service.AutoService;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.Idents;
import net.hollowcube.luau.slopgen.LibraryModelBuilder;
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
        var prop = library.staticProperties().getFirst();
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
        var m = library.staticMethods().getFirst();
        assertEquals("build", m.luaName());
        assertEquals("build", m.javaMethodName());
        assertFalse(m.isVoid());
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
        var ex = library.exports().getFirst();
        assertEquals("Thing", ex.luaName());
        assertTrue(ex.isFinal());
        assertNull(ex.superExport());
        assertFalse(ex.hasSubtypes());
        assertEquals(1, ex.properties().size());
        assertEquals("name", ex.properties().getFirst().luaName());
        assertNull(ex.properties().getFirst().setter());
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
        var ex = library.exports().getFirst();
        assertEquals(1, ex.properties().size(), "getter+setter must merge into one Property");
        var prop = ex.properties().getFirst();
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
        var ex = library.exports().getFirst();
        assertEquals(2, ex.methods().size());
        assertEquals("poke", ex.methods().getFirst().luaName());
        assertTrue(ex.methods().getFirst().isVoid());
        assertEquals("compute", ex.methods().get(1).luaName());
        assertFalse(ex.methods().get(1).isVoid());
    }

    @Test
    void unionAliasStampsKindAndVariantsAndDiscriminator() {
        var library = parseSingle("fixtures.LibProp", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/prop")
            public final class LibProp {
                @LuaExport
                @LuaUnion(discriminator = "kind")
                public static abstract sealed class Prop permits Block, Item {
                    /// @luaReturn string
                    @LuaProperty public int getId(LuaState s) { return 1; }
                }
                @LuaExport
                public static final class Block extends Prop {
                    /// @luaReturn "block"
                    @LuaProperty public int getKind(LuaState s) { return 1; }
                    /// @luaReturn string
                    @LuaProperty public int getBlock(LuaState s) { return 1; }
                }
                @LuaExport
                public static final class Item extends Prop {
                    /// @luaReturn "item"
                    @LuaProperty public int getKind(LuaState s) { return 1; }
                    /// @luaReturn string
                    @LuaProperty public int getItem(LuaState s) { return 1; }
                }
            }
            """);
        var prop = findExport(library, "Prop");
        var block = findExport(library, "Block");
        var item = findExport(library, "Item");
        assertEquals(Model.Export.Kind.UNION_ALIAS, prop.kind());
        assertEquals(Model.Export.Kind.UNION_VARIANT, block.kind());
        assertEquals(Model.Export.Kind.UNION_VARIANT, item.kind());
        assertEquals("kind", prop.discriminator());
        assertEquals(2, prop.unionVariants().size());
        assertEquals(ClassName.get("fixtures", "LibProp", "Block"), prop.unionVariants().get(0));
        assertEquals(ClassName.get("fixtures", "LibProp", "Item"), prop.unionVariants().get(1));
    }

    @Test
    void unionAliasWithoutDiscriminatorLeavesDiscriminatorNull() {
        var library = parseSingle("fixtures.LibUnt", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/unt")
            public final class LibUnt {
                @LuaExport @LuaUnion
                public static abstract sealed class Shape permits Circle, Square {}
                @LuaExport public static final class Circle extends Shape {}
                @LuaExport public static final class Square extends Shape {}
            }
            """);
        var shape = findExport(library, "Shape");
        assertEquals(Model.Export.Kind.UNION_ALIAS, shape.kind());
        assertNull(shape.discriminator());
        assertEquals(2, shape.unionVariants().size());
    }

    @Test
    void unionParentMustBeSealed() {
        var compilation = compile("fixtures.LibUS", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/us")
            public final class LibUS {
                @LuaExport @LuaUnion
                public static abstract class Open {}
            }
            """);
        assertThat(compilation).hadErrorContaining(
            "@LuaUnion requires the class to be `sealed`");
    }

    @Test
    void unionParentMustBeAbstract() {
        var compilation = compile("fixtures.LibUA", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/ua")
            public final class LibUA {
                @LuaExport @LuaUnion
                public static sealed class Concrete permits LibUA.Sub {}
                @LuaExport public static final class Sub extends Concrete {}
            }
            """);
        assertThat(compilation).hadErrorContaining(
            "@LuaUnion requires the class to be `abstract`");
    }

    @Test
    void unionVariantMissingLuaExportIsAnError() {
        var compilation = compile("fixtures.LibUV", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/uv")
            public final class LibUV {
                @LuaExport @LuaUnion
                public static abstract sealed class Root permits LibUV.Plain {}
                public static final class Plain extends Root {}
            }
            """);
        assertThat(compilation).hadErrorContaining("is missing @LuaExport");
    }

    @Test
    void unionVariantSkippingLevelIsAnError() {
        var compilation = compile("fixtures.LibSkip", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/skip")
            public final class LibSkip {
                @LuaExport @LuaUnion
                public static abstract sealed class Top permits LibSkip.Leaf {}
                @LuaExport
                public static non-sealed abstract class Middle extends Top {}
                @LuaExport
                public static final class Leaf extends Middle {}
            }
            """);
        // Top permits Leaf directly, but Leaf's `extends` chain goes through Middle.
        // The validator should flag that Leaf doesn't extend Top directly.
        assertThat(compilation).hadErrorContaining(
            "must extend its union parent 'Top' directly");
    }

    @Test
    void discriminatorMissingPropertyIsAnError() {
        var compilation = compile("fixtures.LibMissD", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/missd")
            public final class LibMissD {
                @LuaExport @LuaUnion(discriminator = "kind")
                public static abstract sealed class Prop permits LibMissD.Block {}
                @LuaExport
                public static final class Block extends Prop {
                    /// @luaReturn string
                    @LuaProperty public int getBlock(LuaState s) { return 1; }
                }
            }
            """);
        assertThat(compilation).hadErrorContaining(
            "to declare a @LuaProperty getter named 'kind'");
    }

    @Test
    void discriminatorNonLiteralTypeIsAnError() {
        var compilation = compile("fixtures.LibNonLit", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/nonlit")
            public final class LibNonLit {
                @LuaExport @LuaUnion(discriminator = "kind")
                public static abstract sealed class Prop permits LibNonLit.Block {}
                @LuaExport
                public static final class Block extends Prop {
                    /// @luaReturn string
                    @LuaProperty public int getKind(LuaState s) { return 1; }
                }
            }
            """);
        assertThat(compilation).hadErrorContaining("to be typed as a string literal");
    }

    @Test
    void duplicateDiscriminatorLiteralIsAnError() {
        var compilation = compile("fixtures.LibDup", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaProperty;
            import net.hollowcube.luau.gen.LuaUnion;
            @LuaLibrary(name = "@t/dup")
            public final class LibDup {
                @LuaExport @LuaUnion(discriminator = "kind")
                public static abstract sealed class Prop permits LibDup.A, LibDup.B {}
                @LuaExport
                public static final class A extends Prop {
                    /// @luaReturn "x"
                    @LuaProperty public int getKind(LuaState s) { return 1; }
                }
                @LuaExport
                public static final class B extends Prop {
                    /// @luaReturn "x"
                    @LuaProperty public int getKind(LuaState s) { return 1; }
                }
            }
            """);
        assertThat(compilation).hadErrorContaining(
            "literal \"x\" is shared by variants 'A' and 'B'");
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
        assertFalse(c.hasSubtypes());
    }

    @Test
    void metaMethodSeparatedFromRegularMethods() {
        var library = parseSingle("fixtures.LibMeta", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/meta")
            public final class LibMeta {
                @LuaExport
                public static final class Vec {
                    @LuaMethod(meta = "__add")
                    public int plus(LuaState state) { return 1; }
                }
            }
            """);
        var ex = library.exports().getFirst();
        assertTrue(ex.methods().isEmpty());
        assertEquals(1, ex.metaMethods().size());
        assertEquals("__add", ex.metaMethods().getFirst().meta());
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
        var ex = library.exports().getFirst();
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
        assertEquals("alpha", atoms.getFirst().luaName());
        assertEquals((short) 1, atoms.getFirst().value());
        assertEquals("beta", atoms.get(1).luaName());
        assertEquals((short) 2, atoms.get(1).value());
    }

    @Test
    void exportTypeLevelGenericLandsOnExport() {
        var library = parseSingle("fixtures.LibEvt", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/evt")
            public final class LibEvt {
                /// @luaGeneric A...
                @LuaExport
                public static final class EventSource {
                    /// @luaParam handler (A...) -> ()
                    @LuaMethod public int listen(LuaState s) { return 0; }
                }
            }
            """);
        var ex = library.exports().getFirst();
        assertEquals(1, ex.generics().size());
        var g = ex.generics().getFirst();
        assertEquals("A", g.name());
        assertTrue(g.pack(), "T... should produce pack=true");
        // Method-level generics are empty — the `A` reference is bound by the type-level decl.
        assertTrue(ex.methods().getFirst().generics().isEmpty());
    }

    @Test
    void exportLevelGenericIsVisibleAcrossEveryMember() {
        // Pin: type-level generics are not duplicated onto each method/accessor; they live on
        // the export and the resolver reads them from there. This keeps generated declarations
        // accurate when projecting to `.d.luau`.
        var library = parseSingle("fixtures.LibAll", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            import net.hollowcube.luau.gen.LuaProperty;
            @LuaLibrary(name = "@t/all")
            public final class LibAll {
                /// @luaGeneric T - the wrapped value
                @LuaExport
                public static final class Box {
                    /// @luaReturn T
                    @LuaProperty public int getValue(LuaState s) { return 1; }
                    /// @luaParam value T
                    @LuaProperty public void setValue(LuaState s) {}
                    /// @luaParam other T
                    /// @luaReturn boolean
                    @LuaMethod public int equals(LuaState s) { return 1; }
                }
            }
            """);
        var ex = library.exports().getFirst();
        assertEquals(1, ex.generics().size());
        assertEquals("T", ex.generics().getFirst().name());
        assertFalse(ex.generics().getFirst().pack());
        // Each member has zero of its own generics; T is inherited from the type.
        assertTrue(ex.methods().getFirst().generics().isEmpty());
        assertEquals(1, ex.properties().size());
    }

    @Test
    void shadowingTypeGenericOnMethodIsAnError() {
        var compilation = compile("fixtures.LibShadow", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/sh")
            public final class LibShadow {
                /// @luaGeneric A...
                @LuaExport
                public static final class Source {
                    /// @luaGeneric A...
                    /// @luaParam handler (A...) -> ()
                    @LuaMethod public int listen(LuaState s) { return 0; }
                }
            }
            """);
        assertThat(compilation).hadErrorContaining(
            "@luaGeneric A shadows the same name on enclosing type 'Source'");
    }

    @Test
    void shadowingScalarGenericOnMethodIsAnError() {
        // Same name, scalar form on both sides — also a shadow.
        var compilation = compile("fixtures.LibShadow2", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/sh2")
            public final class LibShadow2 {
                /// @luaGeneric T
                @LuaExport
                public static final class Holder {
                    /// @luaGeneric T
                    /// @luaParam value T
                    /// @luaReturn T
                    @LuaMethod public int echo(LuaState s) { return 1; }
                }
            }
            """);
        assertThat(compilation).hadErrorContaining(
            "@luaGeneric T shadows the same name on enclosing type 'Holder'");
    }

    @Test
    void distinctMethodGenericNamesAreFine() {
        // The shadow check must not flag method-level generics whose names differ from the
        // type-level ones.
        var library = parseSingle("fixtures.LibCompat", """
            package fixtures;
            import net.hollowcube.luau.LuaState;
            import net.hollowcube.luau.gen.LuaLibrary;
            import net.hollowcube.luau.gen.LuaExport;
            import net.hollowcube.luau.gen.LuaMethod;
            @LuaLibrary(name = "@t/co")
            public final class LibCompat {
                /// @luaGeneric A...
                @LuaExport
                public static final class Source {
                    /// @luaGeneric R
                    /// @luaParam map (A...) -> R
                    /// @luaReturn R
                    @LuaMethod public int collect(LuaState s) { return 1; }
                }
            }
            """);
        var ex = library.exports().getFirst();
        assertEquals(1, ex.generics().size());
        assertEquals("A", ex.generics().getFirst().name());
        assertEquals(1, ex.methods().getFirst().generics().size());
        assertEquals("R", ex.methods().getFirst().generics().getFirst().name());
    }

    @Test
    void typeLevelGenericIsRejectedOnLibraryContainer() {
        // Libraries are static-only; there's nothing to share generics across, so declaring
        // `@luaGeneric` on the library is rejected. (Migration severity = warning.)
        var compilation = compile("fixtures.LibBadGen", """
            package fixtures;
            import net.hollowcube.luau.gen.LuaLibrary;
            /// @luaGeneric T
            @LuaLibrary(name = "@t/badgen")
            public final class LibBadGen {}
            """);
        assertThat(compilation).hadWarningContaining("@luaGeneric is not valid on a library");
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
        return capturing.captured.getFirst();
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
