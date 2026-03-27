package net.hollowcube.mapmaker.editor.terraform;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.mapmaker.map.block.handler.BlockHandlers;
import net.hollowcube.mapmaker.map.block.vanilla.DripleafBlock;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.event.TerraformPreSpawnEntityEvent;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.kyori.adventure.key.Key;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;

import java.util.*;

public class MapServerModule implements TerraformModule {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("map-terraform", EventFilter.INSTANCE)
            .addListener(TerraformPreSpawnEntityEvent.class, this::handleSpawnEntity);

    @Override
    public Set<TerraformStorage> storageTypes() {
        return Set.of(new TerraformStorageHttp());
    }

    @Override
    public Set<EventNode<InstanceEvent>> eventNodes() {
        return Set.of(eventNode);
    }

    @Override
    public Map<Integer, Block> blockStateOverrides() {
        var overrides = new HashMap<Integer, Block>();

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
        register(overrides, Block.ENCHANTING_TABLE, BlockHandlers.ENCHANTING_TABLE);
        register(overrides, Block.DECORATED_POT, BlockHandlers.DECORATED_POT);
        register(overrides, Block.CAMPFIRE, BlockHandlers.CAMPFIRE);
        register(overrides, Block.SOUL_CAMPFIRE, BlockHandlers.CAMPFIRE);
        register(overrides, BlockTags.SHELVES, BlockHandlers.SHELF);

        register(overrides, Block.BIG_DRIPLEAF, DripleafBlock.INSTANCE);

        return overrides;
    }

    private void register(Map<Integer, Block> overrides, Block block, BlockHandler handler) {
        for (var state : block.possibleStates()) {
            overrides.put(state.stateId(), state.withHandler(handler));
        }
    }

    private void register(Map<Integer, Block> overrides, Collection<Key> tag, BlockHandler handler) {
        for (var blockId : tag) {
            var block = Objects.requireNonNull(Block.fromKey(blockId));
            register(overrides, block, handler);
        }
    }

    private void handleSpawnEntity(TerraformPreSpawnEntityEvent event) {
        event.setConstructor(MapEntityType::create);
    }

}
