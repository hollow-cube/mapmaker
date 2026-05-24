package net.hollowcube.scripting.emit;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleLayoutTest {

    @Test
    void fileFor() {
        assertEquals(Path.of("@mapmaker", "init.luau"), ModuleLayout.fileFor("@mapmaker"));
        assertEquals(Path.of("@mapmaker", "task.luau"), ModuleLayout.fileFor("@mapmaker/task"));
        assertEquals(Path.of("@mapmaker", "a", "b.luau"), ModuleLayout.fileFor("@mapmaker/a/b"));
        assertEquals("global.d.luau", ModuleLayout.globalFile());
    }

    @Test
    void relativeRequireSameGroup() {
        assertEquals("./world", ModuleLayout.relativeRequire("@mapmaker/task", "@mapmaker/world"));
        assertEquals("./init", ModuleLayout.relativeRequire("@mapmaker/task", "@mapmaker"));
        assertEquals("./world", ModuleLayout.relativeRequire("@mapmaker", "@mapmaker/world"));
    }

    @Test
    void relativeRequireCrossGroup() {
        assertEquals("../@other/x", ModuleLayout.relativeRequire("@mapmaker/task", "@other/x"));
        assertEquals("../world", ModuleLayout.relativeRequire("@mapmaker/a/b", "@mapmaker/world"));
    }

    @Test
    void localBinding() {
        assertEquals("lib", ModuleLayout.localBinding("@mapmaker"));
        assertEquals("Player", ModuleLayout.localBinding("@mapmaker/player"));
        assertEquals("Task", ModuleLayout.localBinding("@mapmaker/task"));
    }
}
