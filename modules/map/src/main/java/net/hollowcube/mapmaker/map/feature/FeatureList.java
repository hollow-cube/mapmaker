package net.hollowcube.mapmaker.map.feature;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

public class FeatureList {
    private static final Logger logger = LoggerFactory.getLogger(FeatureList.class);

    public static @NotNull FeatureList load(@NotNull ConfigLoaderV3 config) {
        var blockManager = MinecraftServer.getBlockManager();

        var features = new ArrayList<FeatureProvider>();
        try {
            var futures = new ArrayList<CompletableFuture<Void>>();
            for (var feature : ServiceLoader.load(FeatureProvider.class)) {
                features.add(feature);
                for (var blockHandler : feature.blockHandlers()) {
                    blockManager.registerHandler(blockHandler.get().getNamespaceId(), blockHandler);
                }
                futures.add(FutureUtil.fork(() -> feature.init(config)));
            }

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (Exception e) {
            logger.error("Failed to initialize features", e);
            throw new RuntimeException(e);
        }
        return new FeatureList(features);
    }

    private final List<FeatureProvider> features;

    private FeatureList(@NotNull List<FeatureProvider> features) {
        this.features = List.copyOf(features);
    }

    public @NotNull List<FeatureProvider> loadMap(@NotNull MapWorld world) {
        var enabledFeatures = new ArrayList<FeatureProvider>();
        try {
            // Load each feature in parallel
            var enabledFutures = new CompletableFuture[features.size()];
            for (int i = 0; i < features.size(); i++) {
                var feature = features.get(i);
                enabledFutures[i] = FutureUtil.fork(() -> feature.initMap(world));
            }

            CompletableFuture.allOf(enabledFutures).join();

            // Add each feature to the enabled list if it is enabled.
            for (int i = 0; i < features.size(); i++) {
                var feature = features.get(i);
                if ((boolean) enabledFutures[i].get()) {
                    enabledFeatures.add(feature);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Failed to initialize features", e);
            throw new RuntimeException(e);
        }
        return enabledFeatures;
    }

    public void close() {
        features.forEach(FeatureProvider::shutdown);
    }
}
