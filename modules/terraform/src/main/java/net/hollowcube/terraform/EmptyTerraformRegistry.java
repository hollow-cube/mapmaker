package net.hollowcube.terraform;

import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Map;

final class EmptyTerraformRegistry implements TerraformRegistry {
    public static final EmptyTerraformRegistry INSTANCE = new EmptyTerraformRegistry();

    private final Map<String, Block> legacyBlockStates = TerraformRegistryImpl.buildLegacyBlockMap(b -> Block.fromStateId((short) b));

    @Override
    public RegionSelector.@UnknownNullability Factory regionType(@NotNull String id) {
        return null;
    }

    @Override
    public @Nullable TerraformStorage storage(@NotNull String name) {
        return null;
    }

    @Override
    public @UnknownNullability Block blockState(int stateId) {
        return Block.fromStateId((short) stateId);
    }

    @Override
    public @UnknownNullability Block legacyBlockState(int id, int data) {
        return legacyBlockStates.get(String.format("%d:%d", id, data));
    }
}
