package net.hollowcube.luau.engineapi.emit;

import com.google.gson.JsonParser;
import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.Schema;
import net.hollowcube.luau.slopgen.SchemaJson;
import net.hollowcube.luau.slopgen.types.LuauType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EditorJsonTest {

    private static LuauType t(String name) {
        return new LuauType.Named(null, name, List.of());
    }

    @Test
    void embedsGlobalAndModuleTypeStrings() {
        var task = new Model.Library(ClassName.get("p", "Task"), ClassName.get("p", "Task$luau"),
            "@mapmaker/task", LuaLibrary.Scope.REQUIRE, List.of(),
            List.of(new Model.Method("wait", "wait", false, ClassName.get("p", "Task"), "",
                List.of(), List.of(new Model.Param("ticks", true, t("number"), "")),
                List.of(new Model.Return(t("number"), "")))),
            List.of(), "");
        var text = new Model.Library(ClassName.get("p", "T"), ClassName.get("p", "T$luau"),
            "Text", LuaLibrary.Scope.GLOBAL,
            List.of(new Model.Export(ClassName.get("p", "Text"), "Text", null, true, List.of(),
                List.of(), List.of(), List.of(), 1, false, "")),
            List.of(), List.of(), "");

        var libs = new LinkedHashMap<String, Model.Library>();
        libs.put("@mapmaker/task", task);
        libs.put("Text", text);
        var schema = new Schema(SchemaJson.CURRENT_SCHEMA_VERSION, SchemaJson.KIND, libs);

        var root = JsonParser.parseString(EditorJson.build(schema)).getAsJsonObject();

        // The slim editor surface is still present.
        assertTrue(root.has("libraries"));
        // Types are embedded inline.
        var types = root.getAsJsonObject("types");
        assertTrue(types.get("global").getAsString().contains("declare class Text end"));
        var mods = types.getAsJsonObject("modules");
        // Require modules keyed by module name; globals are NOT in modules.
        assertTrue(mods.has("@mapmaker/task"));
        assertFalse(mods.has("Text"));

        // The embedded module text is exactly what the emitter produces.
        var refs = Map.of("p.Text",
            new LibraryModuleEmitter.ExportRef("Text", "Text"));
        assertEquals(new LibraryModuleEmitter(refs).emit(task),
            mods.get("@mapmaker/task").getAsString());
        assertTrue(mods.get("@mapmaker/task").getAsString().contains("function task.wait("));

        // Minified, no HTML escaping of Luau type punctuation.
        String raw = EditorJson.build(schema);
        assertFalse(raw.contains("\n"), "editor JSON must stay minified");
        assertFalse(raw.contains("\\u003c"), "must not HTML-escape '<'");
    }

    @Test
    void globalIsEmptyStringWhenNoGlobalLibraries() {
        var lib = new Model.Library(ClassName.get("p", "L"), ClassName.get("p", "L$luau"),
            "@mapmaker/foo", LuaLibrary.Scope.REQUIRE, List.of(), List.of(), List.of(), "");
        var libs = new LinkedHashMap<String, Model.Library>();
        libs.put("@mapmaker/foo", lib);
        var root = JsonParser.parseString(
                EditorJson.build(new Schema(SchemaJson.CURRENT_SCHEMA_VERSION, SchemaJson.KIND, libs)))
            .getAsJsonObject();
        assertEquals("", root.getAsJsonObject("types").get("global").getAsString());
    }
}
