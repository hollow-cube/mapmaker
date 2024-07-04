package net.hollowcube.mapmaker.map.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.local.config.LocalWorkspace;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.script.MapScriptContainer;
import net.hollowcube.mapmaker.map.world.LocalTestingMapWorld;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@AutoService(FeatureProvider.class)
public class PlayScriptFeatureProvider implements FeatureProvider {
    private Path workspace;

    @Override
    public void init(@NotNull ConfigLoaderV3 config) {
        this.workspace = config.get(LocalWorkspace.class).path();
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof LocalTestingMapWorld))
            return false;

        // this is gross, but this gets called in a virtual thread which is bad because we need this to run on the
        // instance thread. May add some event that runs on instance thread when its starting.
        CompletableFuture<Void> a = new CompletableFuture<>();
        world.instance().scheduleNextTick(_ -> {
            var script = new MapScriptContainer(world, workspace);
            world.instance().setTag(MapScriptContainer.TAG, script);
            a.complete(null);
        });
        a.join();
        return true;
    }

    @Override
    public void cleanupMap(@NotNull MapWorld world) {
        var script = world.instance().getTag(MapScriptContainer.TAG);
        world.instance().removeTag(MapScriptContainer.TAG);

        script.close();
    }
}
