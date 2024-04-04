package net.hollowcube.mapmaker.map.world;

import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AbstractMapMakerMapWorld extends AbstractMapWorld {

    private final FeatureList features;
    private List<FeatureProvider> enabledFeatures;

    protected AbstractMapMakerMapWorld(
            @NotNull MapServer server, @NotNull MapData map,
            @NotNull FeatureList features, @NotNull MapInstance instance) {
        super(server, map, instance);

        this.features = features;
    }

    public @NotNull FeatureList features() {
        return features;
    }

    public @NotNull List<FeatureProvider> enabledFeatures() {
        return enabledFeatures;
    }

    @Override
    public void load() {
        super.load();

        this.enabledFeatures = features.loadMap(this);
    }

    @Override
    public void close(@Nullable Component reason) {
        this.enabledFeatures.forEach(fp -> fp.cleanupMap(this));
        this.enabledFeatures = null;

        super.close(reason);
    }
}
