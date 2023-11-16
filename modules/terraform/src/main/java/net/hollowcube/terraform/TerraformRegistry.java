package net.hollowcube.terraform;

import net.hollowcube.terraform.selection.region.RegionSelector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * An immutable registry of all terraform assets (modules, region types, tools, brushes, etc).
 *
 * <p>All registry keys are lower_snake_case strings. todo enforce this</p>
 */
public final class TerraformRegistry {
    private final Map<String, RegionSelector.Factory> regionTypes;

    TerraformRegistry(@NotNull Collection<TerraformModule> modules) {
        var regionTypes = new HashMap<String, RegionSelector.Factory>();

        for (var module : modules) {
            for (var regionType : module.regionTypes()) {
                regionTypes.put(regionType.id(), regionType);
            }
        }

        this.regionTypes = Map.copyOf(regionTypes);
    }

    public RegionSelector.@UnknownNullability Factory regionType(@NotNull String id) {
        return regionTypes.get(id);
    }

}
