package net.hollowcube.scripting;

import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// End-to-end tests for `@LuaEnum`. Each maps to one of the four spec bullets:
///
///   1. Top-level GLOBAL enum → emitted into `global.d.luau` as `declare class` + `declare T:`,
///      with its own `<Enum>$luau.java` glue.
///   2. Top-level non-GLOBAL enum → compile error.
///   3. Inner enum of a library → `export type` + module field in the library's `.luau`, glue
///      folded into the library's `<Lib>$luau.java`.
///   4. `SCREAMING_SNAKE` constants are renamed to `PascalCase` at the Lua side.
///
/// Plus two corner cases: non-enum annotated with `@LuaEnum` (compile error), and a name override
/// via `@LuaEnum(name = "...")`.
class LuaEnumTest {

    @TempDir
    Path outDir;

    private Compiler compiler() {
        return Compiler.javac()
            .withProcessors(new LuaApiProcessor())
            .withOptions("-A" + LuaApiProcessor.OUTPUT_DIR_OPTION + "=" + outDir.toAbsolutePath());
    }

    @Test
    void topLevelGlobalEnumEmitsAmbientDeclarationAndOwnGlue() throws Exception {
        var compilation = compiler().compile(
            JavaFileObjects.forSourceString("fixtures.Slot", """
                package fixtures;
                import net.hollowcube.scripting.gen.LuaEnum;
                @LuaEnum
                public enum Slot { MAIN_HAND, OFF_HAND, HELMET }
                """));
        assertThat(compilation).succeeded();

        // 1. Glue file exists at the same package as the source enum.
        assertTrue(compilation.generatedSourceFile("fixtures.Slot$luau").isPresent(),
            "expected fixtures.Slot$luau glue file");

        // 2. global.d.luau carries the ambient declaration + constant table.
        var global = Files.readString(outDir.resolve("types/global.d.luau"));
        assertTrue(global.contains("declare class Slot end"),
            "expected nominal `declare class Slot end`. Got:\n" + global);
        assertTrue(global.contains("declare Slot: {"),
            "expected `declare Slot: { ... }` constant table. Got:\n" + global);
        // 4. SCREAMING_SNAKE → PascalCase rename appears in the typed table.
        assertTrue(global.contains("read MainHand: Slot,"),
            "MAIN_HAND should rename to MainHand. Got:\n" + global);
        assertTrue(global.contains("read OffHand: Slot,"),
            "OFF_HAND should rename to OffHand. Got:\n" + global);
        assertTrue(global.contains("read Helmet: Slot,"),
            "HELMET should rename to Helmet (no underscore variant). Got:\n" + global);
    }

    @Test
    void topLevelNonGlobalEnumIsRejected() {
        // The spec disallows REQUIRE scope on a top-level @LuaEnum — there's no library to
        // attach it to. Authors who want a require-scoped enum should nest it inside an
        // @LuaLibrary instead.
        var compilation = compiler().compile(
            JavaFileObjects.forSourceString("fixtures.BadSlot", """
                package fixtures;
                import net.hollowcube.scripting.gen.LuaEnum;
                import net.hollowcube.scripting.gen.LuaLibrary;
                @LuaEnum(scope = LuaLibrary.Scope.REQUIRE)
                public enum BadSlot { A, B }
                """));
        assertThat(compilation).hadErrorContaining("Top-level @LuaEnum must use scope = GLOBAL");
    }

    @Test
    void innerEnumOfLibraryEmitsExportTypeAndModuleField() throws Exception {
        // 3. Inner enum: appears as `export type` and as a typed field on the module return.
        // The library's own glue (LibInv$luau) carries the enum's push/check methods and
        // its constant table on the library metatable.
        var compilation = compiler().compile(
            JavaFileObjects.forSourceString("fixtures.LibInv", """
                package fixtures;
                import net.hollowcube.scripting.gen.LuaEnum;
                import net.hollowcube.scripting.gen.LuaLibrary;
                @LuaLibrary(name = "@t/inv")
                public final class LibInv {
                    @LuaEnum
                    public enum Slot { MAIN_HAND, OFF_HAND }
                }
                """));
        assertThat(compilation).succeeded();

        // Library glue carries the enum push/check + the SLOT_TAG/SLOT_VALUES fields. Java
        // identifiers track the Java class name (`Slot` here, since that's the inner class's
        // simple name); only Lua-visible strings would use the `@LuaEnum(name)` override.
        var libGlue = compilation.generatedSourceFile("fixtures.LibInv$luau")
            .orElseThrow(() -> new AssertionError("missing LibInv$luau"))
            .getCharContent(true).toString();
        assertTrue(libGlue.contains("SLOT_TAG"), "expected SLOT_TAG field. Got:\n" + libGlue);
        assertTrue(libGlue.contains("pushSlot(@NotNull LuaState state, LibInv.Slot value)"),
            "expected push helper. Got:\n" + libGlue);
        assertTrue(libGlue.contains("checkSlotArg(@NotNull LuaState state, int argIndex)"),
            "expected check helper. Got:\n" + libGlue);
        // No separate <Enum>$luau file for inner enums — the glue is folded.
        assertFalse(compilation.generatedSourceFile("fixtures.Slot$luau").isPresent(),
            "inner enums must not emit a separate top-level glue file");

        // The library's .luau carries the export type + module-value entry.
        var libModule = Files.readString(outDir.resolve("types/@t/inv.luau"));
        assertTrue(libModule.contains("export type Slot ="),
            "expected `export type Slot = ...`. Got:\n" + libModule);
        assertTrue(libModule.contains("read Slot:"),
            "expected the module value to expose `Slot: { ... }`. Got:\n" + libModule);
        // 4. SCREAMING_SNAKE → PascalCase rename appears in the module-value cast too.
        assertTrue(libModule.contains("read MainHand: Slot,"),
            "MAIN_HAND should rename to MainHand inside the module. Got:\n" + libModule);
        assertTrue(libModule.contains("read OffHand: Slot,"),
            "OFF_HAND should rename to OffHand inside the module. Got:\n" + libModule);
    }

    @Test
    void luaEnumOnNonEnumIsRejected() {
        var compilation = compiler().compile(
            JavaFileObjects.forSourceString("fixtures.NotAnEnum", """
                package fixtures;
                import net.hollowcube.scripting.gen.LuaEnum;
                @LuaEnum
                public final class NotAnEnum { }
                """));
        assertThat(compilation).hadErrorContaining("@LuaEnum can only be applied to a Java enum");
    }

    @Test
    void enumNameOverrideAppliesToLuaSideOnly() throws Exception {
        // `@LuaEnum(name = "X")` replaces the class's simple name on the LUA side only —
        // ambient declaration, global table key, runtime error label. The Java-side helper
        // identifiers (`pushHandSlot`, `HANDSLOT_TAG`) track the Java class name, matching
        // the existing `@LuaExport` convention where `class Item` produces `pushItem` /
        // `ITEM_TAG` regardless of the Lua module's name.
        var compilation = compiler().compile(
            JavaFileObjects.forSourceString("fixtures.HandSlot", """
                package fixtures;
                import net.hollowcube.scripting.gen.LuaEnum;
                @LuaEnum(name = "Hand")
                public enum HandSlot { LEFT, RIGHT }
                """));
        assertThat(compilation).succeeded();

        // Lua side: override wins.
        var global = Files.readString(outDir.resolve("types/global.d.luau"));
        assertTrue(global.contains("declare class Hand end") && global.contains("declare Hand:"),
            "expected the Lua-side name to be `Hand`. Got:\n" + global);

        // Java side: helper names use the Java class name `HandSlot`.
        var glue = compilation.generatedSourceFile("fixtures.HandSlot$luau")
            .orElseThrow().getCharContent(true).toString();
        assertTrue(glue.contains("HANDSLOT_TAG") && glue.contains("pushHandSlot("),
            "expected HANDSLOT_TAG / pushHandSlot for the Java-side helpers. Got:\n" + glue);
        // And the Lua-visible label is used in the runtime error message.
        assertTrue(glue.contains("\"Hand\""),
            "expected the Lua name `Hand` in the runtime error message. Got:\n" + glue);
    }
}
