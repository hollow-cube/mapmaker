package net.hollowcube.terraform;

import com.google.inject.Injector;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandManager;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.collection.ObjectArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import static net.hollowcube.command.CommandCondition.and;

/**
 * An immutable registry of all terraform assets (modules, region types, tools, brushes, etc).
 *
 * <p>All registry keys are lower_snake_case strings. todo enforce this</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class TerraformRegistry {
    private static final Logger logger = LoggerFactory.getLogger(TerraformRegistry.class);

    private final Map<String, RegionSelector.Factory> regionTypes;
    private final Set<Class<? extends TerraformStorage>> storageTypes;

    private final CommandManager commandManager;

    // Vanilla block registry, kept to allow overriding of vanilla blocks to add a block handler to them
    private final ObjectArray<Block> BLOCK_STATES = ObjectArray.singleThread(16384);

    TerraformRegistry(
            @NotNull Injector injector, @NotNull Collection<Supplier<TerraformModule>> modules,
            @NotNull EventNode<InstanceEvent> rootEventNode,
            @NotNull CommandManager commandManager, @Nullable CommandCondition condition
    ) {
        var regionTypes = new HashMap<String, RegionSelector.Factory>();
        var storageTypes = new HashSet<Class<? extends TerraformStorage>>();

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

            storageTypes.addAll(module.storageTypes());

            for (var eventNode : module.eventNodes()) {
                rootEventNode.addChild(eventNode);
            }

            for (var commandClass : module.commands()) {
                try {
                    var command = injector.getInstance(commandClass);
                    if (condition != null) {
                        var existing = command.getCondition();
                        command.setCondition(existing == null ? condition : and(existing, condition));
                    }
                    commandManager.register(command);
                } catch (Exception e) {
                    logger.error("Failed to register command {}", commandClass.getName(), e);
                }
            }

            for (var state : module.blockStateOverrides().entrySet()) {
                BLOCK_STATES.set(state.getKey(), state.getValue());
                stateOverrides++;
            }
        }

        this.regionTypes = Map.copyOf(regionTypes);
        this.storageTypes = Set.copyOf(storageTypes);
        this.commandManager = commandManager;

        BLOCK_STATES.trim();
        logger.debug("Loaded {} block state overrides", stateOverrides);
    }

    public RegionSelector.@UnknownNullability Factory regionType(@NotNull String id) {
        return regionTypes.get(id);
    }

    public @Nullable Class<? extends TerraformStorage> storage(@NotNull String name) {
        Class<? extends TerraformStorage> type = null;
        for (var storageTypeClass : storageTypes) {
            if (storageTypeClass.getName().endsWith(name)) {
                if (type != null) {
                    throw new IllegalStateException("Storage name matches multiple implementations: " +
                            type.getName() + " and " + storageTypeClass.getName());
                }
                type = storageTypeClass;
            }
        }
        return type;
    }

    public @UnknownNullability Block blockState(int stateId) {
        return BLOCK_STATES.get(stateId);
    }

}
