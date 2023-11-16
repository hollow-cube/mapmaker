package net.hollowcube.map.terraform;

import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.storage.TerraformStorage;
import org.jetbrains.annotations.NotNull;

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
}
