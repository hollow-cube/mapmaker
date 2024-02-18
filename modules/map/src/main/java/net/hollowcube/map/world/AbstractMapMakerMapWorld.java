package net.hollowcube.map.world;

import net.hollowcube.map.feature.FeatureList;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.map2.MapServer;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.map.MapData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class AbstractMapMakerMapWorld extends AbstractMapWorld {

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
    public void close() {
        this.enabledFeatures.forEach(fp -> fp.cleanupMap(this));
        this.enabledFeatures = null;

        super.close();
    }
}
