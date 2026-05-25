package net.hollowcube.scripting.types;

import com.palantir.javapoet.ClassName;
import net.hollowcube.scripting.Model;
import net.hollowcube.scripting.gen.LuaLibrary;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Resolver-level tests for `$Writable<T>`. Construct a `Model.Library` with one method whose
/// single param is `$Writable<X>` for varied X, run the resolver, and inspect the rewritten
/// type. We assert against the parsed AST rather than rendered strings so subtle field-order
/// or modifier changes show up cleanly.
class MetaTypeResolverTest {

    private static LuauType bare(String name) {
        return new LuauType.Named(null, name, List.of());
    }

    private static Model.Accessor accessor(String javaName, ClassName enclosing, LuauType type, boolean isSetter) {
        return new Model.Accessor(javaName, enclosing, "", isSetter ? "value" : null, type);
    }

    /// Build a Library whose only export is `export`, plus one static method
    /// `consume(p: $Writable<argType>)`. Run the resolver and return the post-rewrite type
    /// of that parameter — that's what every test inspects.
    private RewriteResult rewriteAgainst(Model.Export export, LuauType argToWritable) {
        var consumeParam = new Model.Param("p", false,
            new LuauType.Named(null, "$Writable", List.of(new LuauType.TypeArg.Single(argToWritable))),
            "");
        var consume = new Model.Method("consume", "consume", true,
            ClassName.get("fixtures", "Lib"), "", List.of(),
            List.of(consumeParam), List.of());
        var lib = new Model.Library(
            ClassName.get("fixtures", "Lib"),
            ClassName.get("fixtures", "Lib$luau"),
            "@t/lib", LuaLibrary.Scope.REQUIRE,
            List.of(export), List.of(), List.of(consume), List.of(), "");

        var symbols = SymbolTable.build(List.of(lib));
        var diags = new ArrayList<ResolveDiagnostic>();
        var resolver = new MetaTypeResolver(symbols, List.of(lib), diags);
        var rewritten = resolver.rewrite(lib);
        var paramType = rewritten.staticMethods().getFirst().params().getFirst().type();
        return new RewriteResult(paramType, diags);
    }

    private record RewriteResult(LuauType paramType, List<ResolveDiagnostic> diagnostics) {}

    private static Model.Export struct(String name, List<Model.Property> props) {
        var enclosing = ClassName.get("fixtures", "Lib", name);
        return new Model.Export(enclosing, name, null, true,
            List.of(), props, List.of(), List.of(), 1, false,
            Model.Export.Kind.STRUCT, List.of(), null, "");
    }

    // =========================================================================
    // Happy paths
    // =========================================================================

    @Test
    void writableOnStructWithMixedProps() {
        // Foo has read-only `id`, getter+setter `name`, setter-only `secret`. Only the latter
        // two are writable.
        var fooType = ClassName.get("fixtures", "Lib", "Foo");
        var idProp = new Model.Property("id",
            accessor("getId", fooType, bare("string"), false), null);
        var nameProp = new Model.Property("name",
            accessor("getName", fooType, bare("string"), false),
            accessor("setName", fooType, bare("string"), true));
        var secretProp = new Model.Property("secret",
            null,
            accessor("setSecret", fooType, bare("number"), true));
        var foo = struct("Foo", List.of(idProp, nameProp, secretProp));

        var r = rewriteAgainst(foo, bare("Foo"));
        assertTrue(r.diagnostics.isEmpty(), () -> "unexpected diagnostics: " + r.diagnostics);
        var table = assertInstanceOf(LuauType.Table.class, r.paramType);
        assertEquals(2, table.fields().size(), () -> "fields: " + table.fields());
        assertEquals("name", table.fields().get(0).name());
        assertEquals("secret", table.fields().get(1).name());
        var nameField = assertInstanceOf(LuauType.Optional.class, table.fields().get(0).type());
        assertEquals(bare("string"), nameField.inner());
        var secretField = assertInstanceOf(LuauType.Optional.class, table.fields().get(1).type());
        assertEquals(bare("number"), secretField.inner());
        assertNull(table.arrayElement());
        assertNull(table.indexerKey());
    }

    @Test
    void writableOnStructWithNoSetters() {
        var fooType = ClassName.get("fixtures", "Lib", "ReadOnly");
        var foo = struct("ReadOnly", List.of(new Model.Property("id",
            accessor("getId", fooType, bare("string"), false), null)));

        var r = rewriteAgainst(foo, bare("ReadOnly"));
        assertTrue(r.diagnostics.isEmpty());
        var table = assertInstanceOf(LuauType.Table.class, r.paramType);
        assertTrue(table.fields().isEmpty(), "no setters → empty table");
    }

    @Test
    void writableOnInlineTable() {
        // $Writable<{x: number, y: string}> → {x: number?, y: string?}
        var inline = new LuauType.Table(
            List.of(
                new LuauType.TableField("x", bare("number")),
                new LuauType.TableField("y", bare("string"))),
            null, null, null);
        // Need at least one export so the library is valid; an empty one works.
        var dummy = struct("Dummy", List.of());

        var r = rewriteAgainst(dummy, inline);
        assertTrue(r.diagnostics.isEmpty());
        var table = assertInstanceOf(LuauType.Table.class, r.paramType);
        assertEquals(2, table.fields().size());
        assertInstanceOf(LuauType.Optional.class, table.fields().get(0).type());
        assertInstanceOf(LuauType.Optional.class, table.fields().get(1).type());
    }

    // =========================================================================
    // Inheritance
    // =========================================================================

    @Test
    void writableWalksSuperExportChain() {
        // Animal has writable `name`; Dog extends Animal and adds writable `breed`. Expansion
        // of $Writable<Dog> contains both, with Dog's own properties last.
        var animalType = ClassName.get("fixtures", "Lib", "Animal");
        var dogType = ClassName.get("fixtures", "Lib", "Dog");
        var animal = new Model.Export(animalType, "Animal", null, false,
            List.of(),
            List.of(new Model.Property("name", null,
                accessor("setName", animalType, bare("string"), true))),
            List.of(), List.of(), 1, true,
            Model.Export.Kind.STRUCT, List.of(), null, "");
        var dog = new Model.Export(dogType, "Dog", animalType, true,
            List.of(),
            List.of(new Model.Property("breed", null,
                accessor("setBreed", dogType, bare("string"), true))),
            List.of(), List.of(), 2, false,
            Model.Export.Kind.STRUCT, List.of(), null, "");

        // Resolver needs both libraries' exports visible.
        var consumeParam = new Model.Param("p", false,
            new LuauType.Named(null, "$Writable",
                List.of(new LuauType.TypeArg.Single(bare("Dog")))),
            "");
        var consume = new Model.Method("consume", "consume", true,
            ClassName.get("fixtures", "Lib"), "", List.of(),
            List.of(consumeParam), List.of());
        var lib = new Model.Library(
            ClassName.get("fixtures", "Lib"),
            ClassName.get("fixtures", "Lib$luau"),
            "@t/lib", LuaLibrary.Scope.REQUIRE,
            List.of(animal, dog), List.of(), List.of(consume), List.of(), "");
        var symbols = SymbolTable.build(List.of(lib));
        var diags = new ArrayList<ResolveDiagnostic>();
        var resolver = new MetaTypeResolver(symbols, List.of(lib), diags);
        var rewritten = resolver.rewrite(lib);

        assertTrue(diags.isEmpty(), () -> "unexpected diagnostics: " + diags);
        var table = assertInstanceOf(LuauType.Table.class,
            rewritten.staticMethods().getFirst().params().getFirst().type());
        // Ancestor first, child last — `breed` is Dog's own.
        assertEquals(List.of("name", "breed"),
            table.fields().stream().map(LuauType.TableField::name).toList());
    }

    @Test
    void writableOnUnionVariantOk() {
        // A UNION_VARIANT is a concrete struct — $Writable on it works like any STRUCT.
        var variantType = ClassName.get("fixtures", "Lib", "TextProp");
        var variant = new Model.Export(variantType, "TextProp", null, true,
            List.of(),
            List.of(new Model.Property("text", null,
                accessor("setText", variantType, bare("string"), true))),
            List.of(), List.of(), 1, false,
            Model.Export.Kind.UNION_VARIANT, List.of(), null, "");

        var r = rewriteAgainst(variant, bare("TextProp"));
        assertTrue(r.diagnostics.isEmpty());
        var table = assertInstanceOf(LuauType.Table.class, r.paramType);
        assertEquals(1, table.fields().size());
    }

    // =========================================================================
    // Rejections
    // =========================================================================

    @Test
    void writableOnUnionAliasRejected() {
        var aliasType = ClassName.get("fixtures", "Lib", "Prop");
        var alias = new Model.Export(aliasType, "Prop", null, false,
            List.of(),
            List.of(new Model.Property("id",
                accessor("getId", aliasType, bare("string"), false), null)),
            List.of(), List.of(), 1, true,
            Model.Export.Kind.UNION_ALIAS, List.of(), "kind", "");

        var r = rewriteAgainst(alias, bare("Prop"));
        assertEquals(1, r.diagnostics.size());
        assertTrue(r.diagnostics.getFirst().message().contains("@LuaUnion alias"),
            () -> r.diagnostics.getFirst().message());
    }

    @Test
    void writableOnUnknownTypeRejected() {
        var dummy = struct("Dummy", List.of());
        var r = rewriteAgainst(dummy, bare("DoesNotExist"));
        assertEquals(1, r.diagnostics.size());
        assertTrue(r.diagnostics.getFirst().message().contains("DoesNotExist"));
        assertTrue(r.diagnostics.getFirst().message().contains("not a known @LuaExport"));
    }

    @Test
    void writableOnPrimitiveRejected() {
        var dummy = struct("Dummy", List.of());
        var r = rewriteAgainst(dummy, bare("number"));
        assertEquals(1, r.diagnostics.size());
        assertTrue(r.diagnostics.getFirst().message().contains("not a known @LuaExport"));
    }

    @Test
    void writableWithZeroArgsRejected() {
        var dummy = struct("Dummy", List.of());
        // Build a $Writable with no args directly — bypass rewriteAgainst.
        var lib = new Model.Library(
            ClassName.get("fixtures", "Lib"), ClassName.get("fixtures", "Lib$luau"),
            "@t/lib", LuaLibrary.Scope.REQUIRE,
            List.of(dummy),
            List.of(),
            List.of(new Model.Method("consume", "consume", true,
                ClassName.get("fixtures", "Lib"), "", List.of(),
                List.of(new Model.Param("p", false,
                    new LuauType.Named(null, "$Writable", List.of()), "")),
                List.of())),
            List.of(), "");
        var symbols = SymbolTable.build(List.of(lib));
        var diags = new ArrayList<ResolveDiagnostic>();
        new MetaTypeResolver(symbols, List.of(lib), diags).rewrite(lib);
        assertEquals(1, diags.size());
        assertTrue(diags.getFirst().message().contains("takes exactly 1 type argument"));
    }

    @Test
    void unknownMetaTypeRejected() {
        var dummy = struct("Dummy", List.of());
        var lib = new Model.Library(
            ClassName.get("fixtures", "Lib"), ClassName.get("fixtures", "Lib$luau"),
            "@t/lib", LuaLibrary.Scope.REQUIRE,
            List.of(dummy),
            List.of(),
            List.of(new Model.Method("consume", "consume", true,
                ClassName.get("fixtures", "Lib"), "", List.of(),
                List.of(new Model.Param("p", false,
                    new LuauType.Named(null, "$Mystery",
                        List.of(new LuauType.TypeArg.Single(bare("Dummy")))), "")),
                List.of())),
            List.of(), "");
        var symbols = SymbolTable.build(List.of(lib));
        var diags = new ArrayList<ResolveDiagnostic>();
        new MetaTypeResolver(symbols, List.of(lib), diags).rewrite(lib);
        assertEquals(1, diags.size());
        assertTrue(diags.getFirst().message().contains("unknown meta-type '$Mystery'"));
    }

    // =========================================================================
    // No-op cases — `$`-free models pass through unchanged
    // =========================================================================

    @Test
    void modelWithoutMetaTypesIsUnchanged() {
        var fooType = ClassName.get("fixtures", "Lib", "Foo");
        var foo = struct("Foo", List.of(new Model.Property("x",
            accessor("getX", fooType, bare("number"), false), null)));
        var consume = new Model.Method("consume", "consume", true,
            ClassName.get("fixtures", "Lib"), "", List.of(),
            List.of(new Model.Param("p", false, bare("Foo"), "")), List.of());
        var lib = new Model.Library(
            ClassName.get("fixtures", "Lib"), ClassName.get("fixtures", "Lib$luau"),
            "@t/lib", LuaLibrary.Scope.REQUIRE,
            List.of(foo), List.of(), List.of(consume), List.of(), "");
        var symbols = SymbolTable.build(List.of(lib));
        var diags = new ArrayList<ResolveDiagnostic>();
        var rewritten = new MetaTypeResolver(symbols, List.of(lib), diags).rewrite(lib);
        assertTrue(diags.isEmpty());
        assertEquals(bare("Foo"),
            rewritten.staticMethods().getFirst().params().getFirst().type());
    }
}
