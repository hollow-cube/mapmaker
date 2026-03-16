package net.hollowcube.datafix;

import it.unimi.dsi.fastutil.ints.Int2IntMap;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Always represents a single, potentially id mapped, schema. For example, an optimized schema
 * for the `minecraft:chest` block entity will include both chest fixes/properties AND the general
 * block entity fixes/properties.
 *
 * <p>Note that OptimizedSchema never interacts with subversions, however the fix array is always
 * sorted properly to respect subversions.</p>
 */
public record OptimizedSchema(
    String id,
    // If this is an ID mapped schema, holds the children.
    Map<String, OptimizedSchema> idMap,
    // Includes all the versions where this schema or any referenced schemas need to be updated
    // Note that for id mapped schemas this is always the intersection of all child bitsets
    // since we don't know which one will be used.
    BitSet relevantVersions,
    // Data version number to span of fixes, 0 indicates no fixes.
    // A span is represented as `start << 16 | end` where start is an index in fixes.
    Int2IntMap versionToFixSpan,
    // Always sorted by data version and sequential.
    DataFix[] fixes,
    // Always sorted by data version
    List<Property> properties
) {

    public boolean oneshot() {
        return idMap.isEmpty() && properties.isEmpty();
    }

    @Override
    public String toString() {
        return "<" + id + ">";
    }
}
