package net.hollowcube.terraform;

import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Terraform modules are the "extension" layer for terraform.
 *
 * <p>Modules may provide new selection modes, tools, brushes, commands, etc</p>
 */
public interface TerraformModule {

    /**
     * Returns {@link RegionSelector} factories to register.
     */
    default @NotNull Set<RegionSelector.Factory> regionTypes() {
        return Set.of();
    }

    default @NotNull Set<TerraformStorage.Factory> storageTypes() {
        return Set.of();
    }

}
