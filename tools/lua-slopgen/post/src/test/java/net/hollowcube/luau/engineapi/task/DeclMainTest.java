package net.hollowcube.luau.engineapi.task;

import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.engineapi.resolve.Aggregator;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.Schema;
import net.hollowcube.luau.slopgen.SchemaJson;
import net.hollowcube.luau.slopgen.types.LuauType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclMainTest {

    private static LuauType t(String name) {
        return new LuauType.Named(null, name, List.of());
    }

    private static Model.Library require(String module, List<Model.Export> exports,
                                         List<Model.Property> staticProps) {
        return new Model.Library(ClassName.get("p", "L"), ClassName.get("p", "L$luau"),
            module, LuaLibrary.Scope.REQUIRE, exports, List.of(), staticProps, "");
    }

    @Test
    void emitsTreeAndIsDeterministic(@TempDir Path dir) throws Exception {
        var base = require("@mapmaker",
            List.of(new Model.Export(ClassName.get("p", "EventSource"), "EventSource", null, true,
                List.of(new Model.GenericParam("A", true, "")), List.of(), List.of(), List.of(),
                1, false, "")),
            List.of());
        var player = require("@mapmaker/player",
            List.of(new Model.Export(ClassName.get("p", "Player"), "Player", null, true, List.of(),
                List.of(new Model.Property("on_hit",
                    new Model.Accessor("g", ClassName.get("p", "Player"), "", null,
                        new LuauType.Named("@mapmaker", "EventSource", List.of(
                            new LuauType.TypeArg.Single(t("Player"))))), null)),
                List.of(), List.of(), 2, false, "")),
            List.of());
        var text = new Model.Library(ClassName.get("p", "T"), ClassName.get("p", "T$luau"),
            "Text", LuaLibrary.Scope.GLOBAL,
            List.of(new Model.Export(ClassName.get("p", "Text"), "Text", null, true, List.of(),
                List.of(), List.of(), List.of(), 3, false, "")),
            List.of(), List.of(), "");

        var libs = new LinkedHashMap<String, Model.Library>();
        libs.put("@mapmaker", base);
        libs.put("@mapmaker/player", player);
        libs.put("Text", text);
        var schema = new Schema(SchemaJson.CURRENT_SCHEMA_VERSION, SchemaJson.KIND, libs);

        Path schemaFile = dir.resolve("engine-api.json");
        Aggregator.writeSchema(schema, schemaFile);

        Path out1 = dir.resolve("out1");
        DeclMain.main(new String[]{"--schema", schemaFile.toString(), "--output", out1.toString()});

        assertTrue(Files.exists(out1.resolve("global.d.luau")));
        assertTrue(Files.exists(out1.resolve("@mapmaker/init.luau")));
        Path playerFile = out1.resolve("@mapmaker/player.luau");
        assertTrue(Files.exists(playerFile));
        String playerText = Files.readString(playerFile);
        assertTrue(playerText.contains("local lib = require(\"./init\")"), playerText);
        assertTrue(playerText.contains("read on_hit: lib.EventSource<Player>"), playerText);

        // Deterministic: a second run is byte-identical.
        Path out2 = dir.resolve("out2");
        DeclMain.main(new String[]{"--schema", schemaFile.toString(), "--output", out2.toString()});
        assertEquals(playerText, Files.readString(out2.resolve("@mapmaker/player.luau")));
        assertEquals(Files.readString(out1.resolve("global.d.luau")),
            Files.readString(out2.resolve("global.d.luau")));
    }
}
