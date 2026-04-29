package net.hollowcube.luau.slopgen.emit;

/// Maps a Lua atom name (an arbitrary string) to a Java identifier of the form `A_<sanitized>`.
/// The `A_` prefix gives namespace separation and avoids keyword collisions; non-identifier
/// characters in the source name are stripped. Two distinct Lua names that sanitize to the same
/// identifier would collide and must be detected upstream — the current atom set has no such
/// pairs, so collision-disambiguation is a TODO if it ever bites.
public final class AtomNames {

    private AtomNames() {
    }

    public static String javaIdentifier(String luaName) {
        var sb = new StringBuilder(luaName.length() + 2);
        sb.append("A_");
        for (int i = 0; i < luaName.length(); i++) {
            char c = luaName.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') sb.append(c);
        }
        return sb.toString();
    }
}
