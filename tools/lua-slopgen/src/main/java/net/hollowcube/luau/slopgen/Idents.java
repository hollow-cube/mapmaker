package net.hollowcube.luau.slopgen;

import it.unimi.dsi.fastutil.objects.Object2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Mutable accumulator for the identifiers slopgen mints during processing: Lua-name string atoms
/// and userdata tags. Atoms start at 1 (0 is reserved as the runtime "missing" sentinel); userdata
/// tags occupy `1..254` (the Luau VM reserves 0 and 255).
///
/// Backed by a single instance per processor run — the per-library [LuaLibraryProcessor] uses a
/// throwaway and the cross-cutting [LuaAtomTableProcessor] uses the real one.
public final class Idents {
    private final Object2ShortLinkedOpenHashMap<String> atoms = new Object2ShortLinkedOpenHashMap<>();
    private short nextAtom = 1;
    private int nextUserDataTag = 1;
    private int nextLightUserDataTag = 1;

    /// Returns the atom for `luaName`, allocating a new one if not seen before.
    public short atomFor(String luaName) {
        if (atoms.containsKey(luaName)) return atoms.getShort(luaName);
        if (nextAtom == Short.MAX_VALUE) throw new IllegalStateException("Too many atoms!");
        short value = nextAtom++;
        atoms.put(luaName, value);
        return value;
    }

    public boolean isEmpty() {
        return atoms.isEmpty();
    }

    /// Returns atom entries sorted by value (deterministic across runs).
    public List<StringAtom> entries() {
        var out = new ArrayList<StringAtom>(atoms.size());
        for (Object2ShortMap.Entry<String> e : atoms.object2ShortEntrySet())
            out.add(new StringAtom(e.getKey(), e.getShortValue()));
        out.sort(Comparator.comparingInt(StringAtom::value));
        return out;
    }

    public record StringAtom(String luaName, short value) {}

    public int allocUserDataTag() {
        if (nextUserDataTag >= 255) throw new IllegalStateException("Too many userdata tags!");
        return nextUserDataTag++;
    }

    public int allocLightUserDataTag() {
        if (nextLightUserDataTag >= 255) throw new IllegalStateException("Too many light userdata tags!");
        return nextLightUserDataTag++;
    }

}
