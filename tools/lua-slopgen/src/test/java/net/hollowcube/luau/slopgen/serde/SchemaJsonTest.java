package net.hollowcube.luau.slopgen.serde;

import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.Schema;
import net.hollowcube.luau.slopgen.SchemaJson;
import net.hollowcube.luau.slopgen.types.LuauType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaJsonTest {

    @Test
    void emptyLibraryRoundTrip() {
        var lib = new Model.Library(
            ClassName.get("fixtures", "LibEmpty"),
            ClassName.get("fixtures", "LibEmpty$luau"),
            "@t/empty",
            LuaLibrary.Scope.REQUIRE,
            List.of(), List.of(), List.of(),
            "");
        var schema = SchemaJson.fragment(lib);
        var json = SchemaJson.toJson(schema);
        var roundTripped = SchemaJson.read(json);
        assertEquals(schema, roundTripped);
    }

    @Test
    void richModelRoundTrip() {
        var stringT = new LuauType.Named(null, "string", List.of());
        var numberT = new LuauType.Named(null, "number", List.of());
        var nilT = new LuauType.Named(null, "nil", List.of());
        var optionalNumber = new LuauType.Optional(numberT);
        var unionStringNumber = new LuauType.Union(List.of(stringT, numberT));

        var getter = new Model.Accessor(
            "getName", ClassName.get("fixtures", "LibSample", "Animal"),
            "name docs", null, stringT);
        var setter = new Model.Accessor(
            "setName", ClassName.get("fixtures", "LibSample", "Animal"),
            "", "value", stringT);
        var prop = new Model.Property("name", getter, setter);

        var method = new Model.Method(
            "compute", "compute", false,
            ClassName.get("fixtures", "LibSample", "Animal"),
            "compute docs",
            List.of(
                new Model.GenericParam("T", false, "scalar generic"),
                new Model.GenericParam("U", true, "pack generic")),
            List.of(
                new Model.Param("x", false, numberT, "the input"),
                new Model.Param("y", true, optionalNumber, "")),
            List.of(new Model.Return(unionStringNumber, "result of compute")));

        var meta = new Model.MetaMethod(
            "__add", "plus", false,
            "add docs",
            List.of(), List.of(),
            List.of(new Model.Return(numberT, "")));

        var export = new Model.Export(
            ClassName.get("fixtures", "LibSample", "Animal"),
            "Animal", null, false,
            List.of(new Model.GenericParam("E", true, "the event arg pack")),
            List.of(prop), List.of(method), List.of(meta),
            7, true,
            "animal");

        var staticMethod = new Model.Method(
            "build", "build", false,
            ClassName.get("fixtures", "LibSample"),
            "static method docs",
            List.of(), List.of(),
            List.of(new Model.Return(nilT, "")));

        var lib = new Model.Library(
            ClassName.get("fixtures", "LibSample"),
            ClassName.get("fixtures", "LibSample$luau"),
            "@t/sample",
            LuaLibrary.Scope.GLOBAL,
            List.of(export),
            List.of(staticMethod),
            List.of(),
            "library description");

        var json = SchemaJson.toJson(SchemaJson.fragment(lib));

        // The published shape carries no Java implementation detail and renames luaName→name.
        assertFalse(json.contains("\"javaMethodName\""), "javaMethodName must not be published");
        assertFalse(json.contains("\"enclosingType\""), "enclosingType must not be published");
        assertFalse(json.contains("\"isVoid\""), "isVoid must not be published");
        assertFalse(json.contains("\"scope\""), "scope must not be published");
        assertFalse(json.contains("\"luaName\""), "luaName must be published as name");
        // GLOBAL libraries live under `globals`, not the `libraries` object.
        assertTrue(json.contains("\"globals\""), "global library must serialize under globals");

        var roundTripped = SchemaJson.read(json);

        // Read-back restores scope from the collection a library came from, and reconstructs the
        // dropped Java fields with blanks (empty method name, null enclosing type, isVoid derived
        // from an empty returns list). Nothing that consumes the JSON reads those.
        var strippedGetter = new Model.Accessor("", null, "name docs", null, stringT);
        var strippedSetter = new Model.Accessor("", null, "", "value", stringT);
        var strippedProp = new Model.Property("name", strippedGetter, strippedSetter);
        var strippedMethod = new Model.Method(
            "compute", "", false, null, "compute docs",
            method.generics(), method.params(), method.returns());
        var strippedMeta = new Model.MetaMethod(
            "__add", "", false, "add docs",
            List.of(), List.of(), meta.returns());
        var strippedExport = new Model.Export(
            export.javaType(), "Animal", null, false,
            export.generics(),
            List.of(strippedProp), List.of(strippedMethod), List.of(strippedMeta),
            7, true, "animal");
        var strippedStatic = new Model.Method(
            "build", "", false, null, "static method docs",
            List.of(), List.of(), staticMethod.returns());
        var strippedLib = new Model.Library(
            lib.sourceType(), lib.glueType(), "@t/sample", LuaLibrary.Scope.GLOBAL,
            List.of(strippedExport), List.of(strippedStatic), List.of(),
            "library description");

        assertEquals(SchemaJson.fragment(strippedLib), roundTripped);
    }

    @Test
    void multipleLibrariesRoundTrip() {
        Model.Library libA = new Model.Library(
            ClassName.get("a", "LibA"), ClassName.get("a", "LibA$luau"),
            "@a/lib", LuaLibrary.Scope.REQUIRE,
            List.of(), List.of(), List.of(), "a docs");
        Model.Library libB = new Model.Library(
            ClassName.get("b", "LibB"), ClassName.get("b", "LibB$luau"),
            "@b/lib", LuaLibrary.Scope.GLOBAL,
            List.of(), List.of(), List.of(), "b docs");

        var schema = new Schema(SchemaJson.CURRENT_SCHEMA_VERSION, SchemaJson.KIND,
            Map.of("@a/lib", libA, "@b/lib", libB));
        var json = SchemaJson.toJson(schema);
        var roundTripped = SchemaJson.read(json);
        assertEquals(schema.libraries().size(), roundTripped.libraries().size());
        assertEquals(libA, roundTripped.libraries().get("@a/lib"));
        assertEquals(libB, roundTripped.libraries().get("@b/lib"));
    }

    @Test
    void editorJsonIsSlimAndMinified() {
        var stringT = new LuauType.Named(null, "string", List.of());

        var dog = new Model.Export(
            ClassName.get("fixtures", "LibZoo", "Dog"),
            "Dog",
            ClassName.get("fixtures", "LibZoo", "Animal"),
            true,
            List.of(),
            List.of(),
            List.of(new Model.Method(
                "bark", "bark", true, ClassName.get("fixtures", "LibZoo", "Dog"),
                "Bark.", List.of(), List.of(), List.of())),
            List.of(),
            42, false,
            "A dog.");
        var lib = new Model.Library(
            ClassName.get("fixtures", "LibZoo"),
            ClassName.get("fixtures", "LibZoo$luau"),
            "@t/zoo", LuaLibrary.Scope.REQUIRE,
            List.of(dog), List.of(), List.of(),
            "Zoo library.");

        var editor = SchemaJson.toEditorJson(SchemaJson.fragment(lib));

        // Minified: no pretty-print whitespace.
        assertFalse(editor.contains("\n"), "editor JSON must be minified");
        assertFalse(editor.contains("  "), "editor JSON must not be indented");

        // No Java/codegen detail at all.
        for (var k : List.of("sourceType", "glueType", "javaType", "isFinal",
            "userDataTag", "hasSubtypes", "javaMethodName", "enclosingType",
            "isVoid", "scope", "luaName")) {
            assertFalse(editor.contains("\"" + k + "\""), k + " must not appear in editor JSON");
        }

        // superExport is the parent's Luau name, not a Java type.
        assertTrue(editor.contains("\"superExport\":\"Animal\""),
            "superExport must be the parent Luau name");
        assertFalse(editor.contains("fixtures.LibZoo"), "no Java FQCNs in editor JSON");

        // Empty collections / blank strings are pruned (bark has no params/returns/generics).
        assertFalse(editor.contains("[]"), "empty arrays must be pruned");
        assertFalse(editor.contains("\"generics\""), "empty generics must be pruned");
        assertFalse(editor.contains("\"returns\""), "void method has no returns key");

        // Luau surface is preserved.
        assertTrue(editor.contains("\"name\":\"Dog\""));
        assertTrue(editor.contains("\"name\":\"bark\""));
        assertTrue(editor.contains("\"description\":\"A dog.\""));
    }
}
