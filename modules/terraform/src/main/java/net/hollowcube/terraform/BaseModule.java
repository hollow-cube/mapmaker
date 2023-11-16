package net.hollowcube.terraform;

import net.hollowcube.terraform.selection.region.CuboidRegionSelector;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.hollowcube.terraform.storage.TerraformStorageMemory;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * The base terraform module, which provides the default registry assets.
 */
final class BaseModule implements TerraformModule {

    @Override
    public @NotNull Set<RegionSelector.Factory> regionTypes() {
        return Set.of(CuboidRegionSelector.FACTORY);
    }

    @Override
    public @NotNull Set<TerraformStorage.Factory> storageTypes() {
        return Set.of(TerraformStorageMemory.FACTORY);
    }
}
