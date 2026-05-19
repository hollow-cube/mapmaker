package net.hollowcube.luau.engineapi.emit;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.Schema;
import net.hollowcube.luau.slopgen.SchemaJson;

import java.util.HashMap;
import java.util.TreeMap;

/// Builds the editor document: the slim, minified editor JSON (see [SchemaJson#toEditorJson])
/// with the generated Luau type text embedded inline, so the editor app gets the structured API
/// surface and the ready-to-use type stubs from a single file.
///
/// Adds a top-level `types` object:
/// ```
/// "types": {
///   "global": "<global.d.luau text>",          // "" when there are no GLOBAL libraries
///   "modules": { "@mapmaker/task": "<task.luau text>", … }   // keyed by module name
/// }
/// ```
public final class EditorJson {

    private EditorJson() {
    }

    public static String build(Schema schema) {
        var globals = new java.util.ArrayList<Model.Library>();
        var requires = new java.util.ArrayList<Model.Library>();
        var exportsByJavaType = new HashMap<String, LibraryModuleEmitter.ExportRef>();
        for (var lib : schema.libraries().values()) {
            (lib.scope() == LuaLibrary.Scope.GLOBAL ? globals : requires).add(lib);
            for (var ex : lib.exports())
                exportsByJavaType.put(ex.javaType().toString(),
                    new LibraryModuleEmitter.ExportRef(lib.moduleName(), ex.luaName()));
        }

        String global = globals.isEmpty()
            ? ""
            : new GlobalDeclEmitter(exportsByJavaType).emit(globals);

        var libEmitter = new LibraryModuleEmitter(exportsByJavaType);
        // TreeMap → deterministic, module-name-ordered output.
        var modules = new TreeMap<String, String>();
        for (var lib : requires) modules.put(lib.moduleName(), libEmitter.emit(lib));

        var root = JsonParser.parseString(SchemaJson.toEditorJson(schema)).getAsJsonObject();
        var types = new JsonObject();
        types.addProperty("global", global);
        var mods = new JsonObject();
        modules.forEach(mods::addProperty);
        types.add("modules", mods);
        root.add("types", types);

        // Match the editor JSON's writer: compact, no HTML escaping (Luau type syntax uses
        // `<`, `>`, `|`, `&` heavily — escaping them would bloat and obscure the strings).
        return new GsonBuilder().disableHtmlEscaping().create().toJson(root);
    }
}
