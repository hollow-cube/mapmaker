package net.hollowcube.luau.slopgen.model;

import it.unimi.dsi.fastutil.objects.Object2ShortLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Mutable accumulator for Lua-name-to-atom assignments. Atoms are assigned sequentially starting
/// at 1; the value 0 is reserved as a "missing" sentinel by the underlying fastutil map. Callers
/// should not assume a specific assignment order — only that [#atomFor] is stable for repeat lookups
/// of the same name within one [AtomTable] instance.
public final class AtomTable {

    private final Object2ShortLinkedOpenHashMap<String> atoms = new Object2ShortLinkedOpenHashMap<>();
    private short next = 1;

    /// Returns the atom for `luaName`, allocating a new one if not seen before.
    public short atomFor(String luaName) {
        if (atoms.containsKey(luaName)) return atoms.getShort(luaName);
        if (next == Short.MAX_VALUE) throw new IllegalStateException("Too many atoms!");
        short value = next++;
        atoms.put(luaName, value);
        return value;
    }

    public boolean isEmpty() {
        return atoms.isEmpty();
    }

    /// Returns entries sorted by atom value (deterministic across runs).
    public List<Entry> entries() {
        var out = new ArrayList<Entry>(atoms.size());
        for (Object2ShortMap.Entry<String> e : atoms.object2ShortEntrySet())
            out.add(new Entry(e.getKey(), e.getShortValue()));
        out.sort(Comparator.comparingInt(Entry::value));
        return out;
    }

    public record Entry(String luaName, short value) {}
}
