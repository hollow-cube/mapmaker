package net.hollowcube.map.terraform;

import net.hollowcube.map.block.handler.BannerBlockHandler;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

        overrides.put(Block.BLUE_WALL_BANNER.stateId(), Block.BLUE_WALL_BANNER.withHandler(BannerBlockHandler.INSTANCE));

        return overrides;
    }

}
