package net.hollowcube.map.terraform;

import net.hollowcube.map.block.BlockTags;
import net.hollowcube.map.block.handler.BlockHandlers;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class MapServerModule implements TerraformModule {

    @Override
    public @NotNull Set<TerraformStorage.Factory> storageTypes() {
        var mapServiceUrl = System.getenv("MAPMAKER_MAP_SERVICE_URL");
        if (mapServiceUrl == null) mapServiceUrl = "http://localhost:9125";
        final var mapServiceUrlFinal = mapServiceUrl;

        return Set.of(
                new TerraformStorage.Factory("http", () -> new TerraformStorageHttp(mapServiceUrlFinal))
        );
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

}
