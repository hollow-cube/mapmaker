package net.hollowcube.terraform;

import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.UnknownNullability;

public sealed interface TerraformRegistry permits EmptyTerraformRegistry, TerraformRegistryImpl {
    @TestOnly
    @NotNull TerraformRegistry EMPTY = EmptyTerraformRegistry.INSTANCE;

    RegionSelector.@UnknownNullability Factory regionType(@NotNull String id);

    @Nullable Class<? extends TerraformStorage> storage(@NotNull String name);

    @UnknownNullability Block blockState(int stateId);

    @UnknownNullability Block legacyBlockState(int id, int data);
}
