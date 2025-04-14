package net.hollowcube.datafix.data;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.hollowcube.datafix.FixFunc;
import net.hollowcube.datafix.Property;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.List;

/**
 * Always represents a single, potentially id mapped, schema. For example, an optimized schema
 * for the `minecraft:chest` block entity will include both chest fixes/properties AND the general
 * block entity fixes/properties.
 *
 * <p>Note that OptimizedSchema never interacts with subversions, however the fix array is always
 * sorted properly to respect subversions.</p>
 */
public record OptimizedSchema(
        @NotNull String id, boolean isIdMapped,
        // Includes all the versions where this schema or any referenced schemas need to be updated
        @NotNull BitSet relevantVersions,
        // Data version number to span of fixes, 0 indicates no fixes.
        // A span is represented as `start << 16 | end` where start is an index in fixes.
        @NotNull Int2IntMap versionToFixSpan,
        // Always sorted by data version and sequential.
        @NotNull FixFunc[] fixes,
        // Always sorted by data version
        @NotNull List<Property> properties
) {
    // TODO for ID changes, the ID in this schema could represent the ID of this exact schema
    //  not including the parent (eg `minecraft:chest`) and we can simply compare to that rather
    //  than worry about tracking that in the fix loop.

    // TODO: fastpath for non-id mapped schemas with no properties.
    //  we can just get the start and end index in fixes and run them all with no
    //  concerns since the type itself can never change.
    public boolean oneshot() {
        return !isIdMapped && properties.isEmpty();
    }

    @Override
    public String toString() {
        return "<" + id + ">";
    }
}
