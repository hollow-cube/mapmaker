package net.hollowcube.terraform.legacy;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.apply.HeightmapView;
import net.hollowcube.terraform.legacy.selection.region.RegionSelector;
import net.hollowcube.terraform.legacy.storage.TerraformStorage;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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

    default @NotNull Set<TerraformStorage> storageTypes() {
        return Set.of();
    }

    default @NotNull Set<EventNode<InstanceEvent>> eventNodes() {
        return Set.of();
    }

    /**
     * Returns any commands to register.
     */
    default @NotNull Set<CommandDsl> commands() {
        return Set.of();
    }

    /**
     * Returns any block state overrides. When terraform attempts to place this state, it will be replaced with
     * the block returned by this map.
     *
     * <p>If a block is registered from two modules, only the second will be used.</p>
     *
     * @return A set of block state overrides to apply
     */
    default @NotNull Map<@NotNull Integer, @NotNull Block> blockStateOverrides() {
        return Map.of();
    }

    /**
     * Returns the heightmaps an apply maintains for each chunk it writes, or null to leave them to another module
     * (Minestom's own heightmaps when no module provides any). At most one module may provide them.
     */
    default @Nullable Function<Chunk, Collection<HeightmapView>> heightmaps() {
        return null;
    }

}
