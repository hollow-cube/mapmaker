package net.hollowcube.map.terraform;

import net.hollowcube.map.block.BlockTags;
import net.hollowcube.map.block.handler.BlockHandlers;
import net.hollowcube.map.entity.MapEntityType;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.event.TerraformPreSpawnEntityEvent;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class MapServerModule implements TerraformModule {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("map-terraform", EventFilter.INSTANCE)
            .addListener(TerraformPreSpawnEntityEvent.class, this::handleSpawnEntity);

    @Override
    public @NotNull Set<Class<? extends TerraformStorage>> storageTypes() {
        return Set.of(TerraformStorageHttp.class);
    }

    @Override
    public @NotNull Set<EventNode<InstanceEvent>> eventNodes() {
        return Set.of(eventNode);
    }

    @Override
    public @NotNull Map<@NotNull Short, @NotNull Block> blockStateOverrides() {
        var overrides = new HashMap<Short, Block>();

        register(overrides, BlockTags.SIGNS, BlockHandlers.SIGN);
        register(overrides, BlockTags.ALL_HANGING_SIGNS, BlockHandlers.HANGING_SIGN);
        register(overrides, BlockTags.BANNERS, BlockHandlers.BANNER);
        register(overrides, Block.CHEST, BlockHandlers.CHEST);
        register(overrides, Block.TRAPPED_CHEST, BlockHandlers.TRAPPED_CHEST);
        register(overrides, BlockTags.SHULKER_BOXES, BlockHandlers.SHULKER_BOX);
        register(overrides, Block.SPAWNER, BlockHandlers.MONSTER_SPAWNER);
        register(overrides, Block.END_PORTAL, BlockHandlers.END_PORTAL);
        register(overrides, Block.ENDER_CHEST, BlockHandlers.ENDER_CHEST);
        register(overrides, BlockTags.SKULLS, BlockHandlers.MOB_HEAD);
        register(overrides, Block.PLAYER_HEAD, BlockHandlers.PLAYER_HEAD);
        register(overrides, Block.PLAYER_WALL_HEAD, BlockHandlers.PLAYER_HEAD);
        register(overrides, BlockTags.BEDS, BlockHandlers.BED);
        register(overrides, Block.CONDUIT, BlockHandlers.CONDUIT);
        register(overrides, Block.BELL, BlockHandlers.BELL);
        register(overrides, Block.DECORATED_POT, BlockHandlers.DECORATED_POT);

        return overrides;
    }

    private void register(Map<Short, Block> overrides, Block block, BlockHandler handler) {
        for (var state : block.possibleStates()) {
            overrides.put(state.stateId(), state.withHandler(handler));
        }
    }

    private void register(Map<Short, Block> overrides, Collection<NamespaceID> tag, BlockHandler handler) {
        for (var blockId : tag) {
            var block = Objects.requireNonNull(Block.fromNamespaceId(blockId));
            register(overrides, block, handler);
        }
    }

    private void handleSpawnEntity(@NotNull TerraformPreSpawnEntityEvent event) {
        event.setConstructor(MapEntityType::create);
    }

}
