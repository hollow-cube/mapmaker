package net.hollowcube.datafix;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.datafix.util.Value;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

sealed class DataTypeImpl implements DataType permits DataTypeIDMappedImpl {
    private final DataTypeIDMappedImpl parent;
    private final Key key;

    // Mapping of path string to property
    private final List<Property> properties = new ArrayList<>();
    // TODO: in the future the array should be dense and ordered by fix version
    //  then separately maintain a mapping of fix version to index range perhaps, idk.
    private final List<Pair<Integer, Function<Value, Value>>> fixes = new ArrayList<>();

    // Post build/cached state: todo i wonder if we just delete the building state and only keep the "optimized" state after?
    // like turn DataType into purely a reference ID.
    public BitSet relevantVersions;

    private DataTypeImpl(@Nullable DataTypeIDMappedImpl parent, @NotNull Key key) {
        this.parent = parent;
        this.key = key;

        DataFixes.addDataType(this);
    }

    public DataTypeImpl(@NotNull Key key) {
        this(null, key);
    }

    public DataTypeImpl(@NotNull DataTypeIDMappedImpl parent, @NotNull String id) {
        this(parent, computeKey(parent.key(), id));
    }

    @Override
    public @NotNull Key key() {
        return this.key;
    }

    public void addProperty(@NotNull Property property) {
        properties.add(property);
    }

    public @NotNull List<Property> properties() {
        return properties;
    }

    public void addFix(int version, @NotNull Function<Value, Value> fix) {
        fixes.add(Pair.of(version, fix));
    }

    public List<Pair<Integer, Function<Value, Value>>> fixes() {
        return fixes;
    }

    public void optimize() {
        if (this.relevantVersions != null) return; // Already done.

        //TODO is it ever worth skipping to the start offset? like we always have 99 empty values to start with which i guess is dumb.

        if (parent != null) {
            parent.optimize();
            fixes.addAll(parent.fixes());
        }

        var maxVersion = 0;
        for (var fix : fixes) {
            maxVersion = Math.max(maxVersion, fix.first());
        }

        relevantVersions = new BitSet(maxVersion);
        for (var fix : fixes) {
            relevantVersions.set(fix.first());
        }
        if (parent != null) {
            relevantVersions.or(parent.relevantVersions);
        }

    }

    private static Key computeKey(@NotNull Key parent, @NotNull String id) {
        try {
            var childKey = Key.key(id);
            return Key.key(parent.namespace(), parent.value() + "/" +
                    (childKey.namespace().equals(parent.namespace()) ? "" : childKey.namespace() + "/") + childKey.value());
        } catch (InvalidKeyException ignored) {
            // Its a legacy one so prefix the path
            return Key.key(parent.namespace(), parent.value() + "/legacy/" + id.toLowerCase(Locale.ROOT));
        }
    }
}
