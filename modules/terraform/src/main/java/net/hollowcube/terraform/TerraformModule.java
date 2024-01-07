package net.hollowcube.terraform;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
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

    default @NotNull Set<EventNode<InstanceEvent>> eventNodes() {
        return Set.of();
    }

    /**
     * Returns any commands to register.
     *
     * @param terraform The terraform instance being loaded. <b>Note that the instance has
     *                  not been fully initialized at this point, it should only be stored.</b>
     */
    default @NotNull Set<CommandDsl> commands(@NotNull Terraform terraform) {
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
    default @NotNull Map<@NotNull Short, @NotNull Block> blockStateOverrides() {
        return Map.of();
    }

}
