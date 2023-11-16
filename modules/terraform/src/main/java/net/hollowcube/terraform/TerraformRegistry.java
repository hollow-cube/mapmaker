package net.hollowcube.terraform;

import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
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
    private final Map<String, TerraformStorage.Factory> storageTypes;

    TerraformRegistry(@NotNull Collection<TerraformModule> modules) {
        var regionTypes = new HashMap<String, RegionSelector.Factory>();
        var storageTypes = new HashMap<String, TerraformStorage.Factory>();

        for (var module : modules) {
            for (var regionType : module.regionTypes()) {
                regionTypes.put(regionType.id(), regionType);
            }

            for (var storageType : module.storageTypes()) {
                storageTypes.put(storageType.name(), storageType);
            }
        }

        this.regionTypes = Map.copyOf(regionTypes);
        this.storageTypes = Map.copyOf(storageTypes);
    }

    public RegionSelector.@UnknownNullability Factory regionType(@NotNull String id) {
        return regionTypes.get(id);
    }

    public TerraformStorage.@UnknownNullability Factory storage(@NotNull String name) {
        return storageTypes.get(name);
    }

}
