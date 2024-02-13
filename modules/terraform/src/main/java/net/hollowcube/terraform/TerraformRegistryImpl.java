package net.hollowcube.terraform;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Injector;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandManager;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.collection.ObjectArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static net.hollowcube.command.CommandCondition.and;

/**
 * An immutable registry of all terraform assets (modules, region types, tools, brushes, etc).
 *
 * <p>All registry keys are lower_snake_case strings. todo enforce this</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class TerraformRegistryImpl implements TerraformRegistry {
    private static final Logger logger = LoggerFactory.getLogger(TerraformRegistry.class);

    private final Map<String, RegionSelector.Factory> regionTypes;
    private final Set<Class<? extends TerraformStorage>> storageTypes;

    private final CommandManager commandManager;

    // Vanilla block registry, kept to allow overriding of vanilla blocks to add a block handler to them
    private final ObjectArray<Block> blocksByState = ObjectArray.singleThread(16384);
    private final Map<String, Block> legacyBlockStates;

    TerraformRegistryImpl(
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
            blocksByState.set(i, block);
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
                blocksByState.set(state.getKey(), state.getValue());
                stateOverrides++;
            }
        }

        this.regionTypes = Map.copyOf(regionTypes);
        this.storageTypes = Set.copyOf(storageTypes);
        this.commandManager = commandManager;

        blocksByState.trim();
        legacyBlockStates = buildLegacyBlockMap(blocksByState::get);
        logger.debug("Loaded {} block state overrides", stateOverrides);
    }

    @Override
    public RegionSelector.@UnknownNullability Factory regionType(@NotNull String id) {
        return regionTypes.get(id);
    }

    @Override
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

    @Override
    public @UnknownNullability Block blockState(int stateId) {
        return blocksByState.get(stateId);
    }

    @Override
    public @UnknownNullability Block legacyBlockState(int id, int data) {
        return legacyBlockStates.get(String.format("%d:%d", id, data));
    }

    static @NotNull Map<String, Block> buildLegacyBlockMap(@NotNull IntFunction<Block> getBlockByState) {
        JsonObject legacyBlockMap;
        try (var is = TerraformRegistryImpl.class.getResourceAsStream("/legacy_ids.json")) {
            if (is == null) {
                logger.warn("Failed to load legacy block map: resource not found");
                return Map.of();
            }

            var legacyData = new Gson().fromJson(new InputStreamReader(is), JsonObject.class);
            legacyBlockMap = legacyData.getAsJsonObject("blocks");
            if (legacyBlockMap == null) {
                logger.warn("Failed to load legacy block map: no blocks object");
                return Map.of();
            }
        } catch (Exception e) {
            logger.warn("Failed to load legacy block map: {}", e.getMessage());
            return Map.of();
        }

        var result = new HashMap<String, Block>();
        for (var entry : legacyBlockMap.entrySet()) {
            var raw = entry.getValue().getAsString();
            Block blockState;
            try {
                //noinspection deprecation
                blockState = ArgumentBlockState.staticParse(raw);
                blockState = getBlockByState.apply(blockState.stateId());
            } catch (ArgumentSyntaxException e) {
                logger.warn("Failed to parse legacy block state '{}': {}", raw, e.getMessage());
                continue;
            }

            result.put(entry.getKey(), blockState);
        }

        return Map.copyOf(result);
    }

}
