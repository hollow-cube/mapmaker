package net.hollowcube.terraform;

import net.hollowcube.terraform.selection.region.RegionSelector;
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

}
