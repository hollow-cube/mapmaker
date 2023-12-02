package net.hollowcube.terraform;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandManager;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.collection.ObjectArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * An immutable registry of all terraform assets (modules, region types, tools, brushes, etc).
 *
 * <p>All registry keys are lower_snake_case strings. todo enforce this</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class TerraformRegistry {
    private static final Logger logger = LoggerFactory.getLogger(TerraformRegistry.class);

    private final Map<String, RegionSelector.Factory> regionTypes;
    private final Map<String, TerraformStorage.Factory> storageTypes;

    private final CommandManager commandManager;

    // Vanilla block registry, kept to allow overriding of vanilla blocks to add a block handler to them
    private final ObjectArray<Block> BLOCK_STATES = ObjectArray.singleThread(16384);

    TerraformRegistry(
            @NotNull TerraformImpl tf, @NotNull Collection<Supplier<TerraformModule>> modules,
            @NotNull CommandManager commandManager, @Nullable CommandCondition condition
    ) {
        var regionTypes = new HashMap<String, RegionSelector.Factory>();
        var storageTypes = new HashMap<String, TerraformStorage.Factory>();

        // Copy vanilla blocks by their state IDs
        for (short i = 0; i < Short.MAX_VALUE; i++) {
            var block = Block.fromStateId(i);
            if (block == null) break; // Reached end
            BLOCK_STATES.set(i, block);
        }

        int stateOverrides = 0;
        for (var moduleFunc : modules) {
            var module = moduleFunc.get();

            for (var regionType : module.regionTypes()) {
                regionTypes.put(regionType.id(), regionType);
            }

            for (var storageType : module.storageTypes()) {
                storageTypes.put(storageType.name(), storageType);
            }

            for (var eventNode : module.eventNodes()) {
                tf.eventNode.addChild(eventNode);
            }

            for (var command : module.commands(tf)) {
                if (condition != null) command.setCondition(condition);
                commandManager.register(command);
            }

            for (var state : module.blockStateOverrides().entrySet()) {
                BLOCK_STATES.set(state.getKey(), state.getValue());
                stateOverrides++;
            }
        }

        this.regionTypes = Map.copyOf(regionTypes);
        this.storageTypes = Map.copyOf(storageTypes);
        this.commandManager = commandManager;

        BLOCK_STATES.trim();
        logger.debug("Loaded {} block state overrides", stateOverrides);
    }

    public RegionSelector.@UnknownNullability Factory regionType(@NotNull String id) {
        return regionTypes.get(id);
    }

    public TerraformStorage.@UnknownNullability Factory storage(@NotNull String name) {
        return storageTypes.get(name);
    }

    public @UnknownNullability Block blockState(int stateId) {
        return BLOCK_STATES.get(stateId);
    }

}
