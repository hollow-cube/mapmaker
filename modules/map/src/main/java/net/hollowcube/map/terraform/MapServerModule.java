package net.hollowcube.map.terraform;

import net.hollowcube.map.block.BlockTags;
import net.hollowcube.map.block.handler.*;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

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

        BiConsumer<Block, BlockHandler> override = (block, handler) -> {
            for (var state : block.possibleStates()) {
                overrides.put(state.stateId(), state.withHandler(handler));
            }
        };

        for (var banner : BlockTags.BANNERS) {
            override.accept(Objects.requireNonNull(Block.fromNamespaceId(banner)), BannerBlockHandler.INSTANCE);
        }
        override.accept(Block.CHEST, ChestBlockHandler.CHEST);
        override.accept(Block.TRAPPED_CHEST, ChestBlockHandler.TRAPPED_CHEST);
        override.accept(Block.PLAYER_HEAD, PlayerHeadBlockHandler.INSTANCE);
        override.accept(Block.PLAYER_WALL_HEAD, PlayerHeadBlockHandler.INSTANCE);
        for (var skull : BlockTags.SKULLS) {
            override.accept(Objects.requireNonNull(Block.fromNamespaceId(skull)), SkullBlockHandler.INSTANCE);
        }
        for (var shulkerBox : BlockTags.SHULKER_BOXES) {
            override.accept(Objects.requireNonNull(Block.fromNamespaceId(shulkerBox)), ShulkerBoxBlockHandler.INSTANCE);
        }
        for (var sign : BlockTags.ALL_SIGNS) {
            override.accept(Objects.requireNonNull(Block.fromNamespaceId(sign)), SignBlockHandler.INSTANCE);
        }
        override.accept(Block.CONDUIT, ConduitBlockHandler.INSTANCE);

        return overrides;
    }

}
