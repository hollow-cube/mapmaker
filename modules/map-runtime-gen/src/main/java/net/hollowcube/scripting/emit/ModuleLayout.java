package net.hollowcube.scripting.emit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Maps a `@LuaLibrary` require-scope module name to its on-disk location in the generated
/// bundle, and computes the relative `require(...)` spec between two modules.
///
/// Layout: a module name is split on `/`. The first segment is the group directory, the last
/// segment is the file. A bare group module (`@mapmaker`, no sub-path) becomes the directory's
/// `init` file so a directory require resolves it. Examples:
///
///  - `@mapmaker`        → `@mapmaker/init.luau`
///  - `@mapmaker/task`   → `@mapmaker/task.luau`
///  - `@mapmaker/a/b`    → `@mapmaker/a/b.luau`
///
/// Requires are relative so the emitted bundle is self-contained (no `.luaurc` alias needed):
/// `@mapmaker/task` → `@mapmaker/world` is `./world`; → `@mapmaker` is `./init`;
/// → `@other/x` is `../@other/x`.
public final class ModuleLayout {

    private ModuleLayout() {
    }

    public static final String GLOBAL_FILE = "global.d.luau";

    /// Path segments of a module's file with the extension dropped. A bare group gets an
    /// explicit `init` filename: `@mapmaker` → [`@mapmaker`,`init`].
    private static List<String> segments(String moduleName) {
        var parts = new ArrayList<String>();
        for (var p : moduleName.split("/")) {
            if (!p.isEmpty()) parts.add(p);
        }
        if (parts.isEmpty()) throw new IllegalArgumentException("empty module name");
        if (parts.size() == 1) parts.add("init");
        return parts;
    }

    /// Bundle-relative file path for a require-scope library, e.g. `@mapmaker/task.luau`.
    public static Path fileFor(String moduleName) {
        var segs = segments(moduleName);
        var dirs = segs.subList(0, segs.size() - 1);
        String file = segs.get(segs.size() - 1) + ".luau";
        Path p = Path.of(dirs.get(0));
        for (int i = 1; i < dirs.size(); i++) p = p.resolve(dirs.get(i));
        return p.resolve(file);
    }

    /// The single ambient definitions file for all GLOBAL-scope libraries.
    public static String globalFile() {
        return GLOBAL_FILE;
    }

    /// `require(...)` argument to import `toModule` from inside `fromModule`'s file. Always a
    /// relative spec beginning with `./` or `../`, with the `.luau` extension dropped.
    public static String relativeRequire(String fromModule, String toModule) {
        var from = segments(fromModule);
        var to = segments(toModule);
        var fromDir = from.subList(0, from.size() - 1);

        int max = Math.min(fromDir.size(), to.size() - 1);
        int common = 0;
        while (common < max && fromDir.get(common).equals(to.get(common))) common++;

        var sb = new StringBuilder();
        int ups = fromDir.size() - common;
        for (int i = 0; i < ups; i++) sb.append("../");
        if (ups == 0) sb.append("./");
        for (int i = common; i < to.size(); i++) {
            if (i > common) sb.append('/');
            sb.append(to.get(i));
        }
        return sb.toString();
    }

    /// Natural local binding identifier for an imported module: the capitalized last path
    /// segment, or `lib` for a bare group module. Callers must de-duplicate collisions.
    public static String localBinding(String moduleName) {
        var segs = segments(moduleName);
        String last = segs.get(segs.size() - 1);
        if (last.equals("init")) return "lib";
        var sb = new StringBuilder();
        for (int i = 0; i < last.length(); i++) {
            char c = last.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') sb.append(c);
        }
        if (sb.isEmpty()) return "mod";
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        if (Character.isDigit(sb.charAt(0))) sb.insert(0, '_');
        return sb.toString();
    }
}
