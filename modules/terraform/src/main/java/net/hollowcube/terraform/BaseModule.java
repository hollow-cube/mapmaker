package net.hollowcube.terraform;

import net.hollowcube.terraform.selection.region.CuboidRegionSelector;
import net.hollowcube.terraform.selection.region.RegionSelector;
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

}
