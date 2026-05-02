package net.hollowcube.luau.slopgen.serde;

import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.Meta;
import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.Schema;
import net.hollowcube.luau.slopgen.SchemaJson;
import net.hollowcube.luau.slopgen.types.LuauType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            Meta.ADD, "plus", false,
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

        var schema = SchemaJson.fragment(lib);
        var json = SchemaJson.toJson(schema);
        var roundTripped = SchemaJson.read(json);
        assertEquals(schema, roundTripped);
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
}
