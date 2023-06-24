package net.hollowcube.mapmaker.hub.find_a_new_home.legacy;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import kotlin.Pair;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.world.storage.FileStorageS3;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public @Blocking class LegacyMapService {
    private static final System.Logger logger = System.getLogger(LegacyMapService.class.getSimpleName());

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();
    private static final Gson GSON = new Gson();
    private static final String LEGACY_MAPS_BUCKET_NAME = "legacy-maps";

    public static @Blocking @NotNull LegacyMapService create(@NotNull String uri) {
        return new LegacyMapService(FileStorageS3.connectS3(uri).getFirst());
    }

    // Map of uuid to a future of their legacy maps (to handle inflight requests)
    private final Map<String, Future<List<LegacyMap>>> legacyMapCache = new ConcurrentHashMap<>();
    private final AmazonS3 s3;

    private LegacyMapService(@NotNull AmazonS3 s3) {
        this.s3 = s3;
    }

    public @Blocking @NotNull List<LegacyMap> getMapsForUuid(@NotNull String uuid) {
        var legacyMapsFuture = legacyMapCache.get(uuid);
        if (legacyMapsFuture == null) {
            legacyMapsFuture = EXECUTOR_SERVICE.submit(() -> getMapListFromS3(uuid));
            legacyMapCache.put(uuid, legacyMapsFuture);
        }

        try {
            return legacyMapsFuture.get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // getCause because it is actually an ExecutionException
            if (e.getCause() instanceof AmazonS3Exception ex && ex.getStatusCode() == 404) {
                return List.of();
            }
            logger.log(System.Logger.Level.ERROR, "Error getting legacy maps for uuid " + uuid, e);
            throw new RuntimeException(e);
        }
    }

    // returns the slot and map data
    @Blocking
    public @NotNull Pair<@NotNull MapData, Integer> importMap(@NotNull LegacyMap map, @NotNull String mapId) {
        var ownerUuid = map.creatorUuid();
        var srcObject = String.format("%s/%s/%s.zip.zst", ownerUuid.substring(0, 2), ownerUuid, map.id());
        s3.copyObject(LEGACY_MAPS_BUCKET_NAME, srcObject, "mapmaker", mapId);
        return null;
    }

    private @NotNull List<LegacyMap> getMapListFromS3(@NotNull String uuid) throws IOException {
        var fileName = String.format("%s/%s/data.json", uuid.substring(0, 2), uuid);
        try (var file = s3.getObject(LEGACY_MAPS_BUCKET_NAME, fileName)) {
            var data = GSON.fromJson(new InputStreamReader(file.getObjectContent()), JsonObject.class);

            var maps = new ArrayList<LegacyMap>();
            for (var entry : data.entrySet()) {
                maps.add(new LegacyMap(entry.getValue().getAsJsonObject()));
            }
            return maps;
        }
    }

}
