package net.hollowcube.luau.slopgen.model;

/// Allocates 8-bit userdata tag values for `@LuaExport` types. Luau's tag space is bounded at 254
/// (1..254 inclusive; 0 and 255 are reserved by the VM).
public final class UserDataTagTable {

    private int next = 1;

    public int allocate() {
        if (next >= 255) throw new IllegalStateException("Too many userdata tags!");
        return next++;
    }
}
