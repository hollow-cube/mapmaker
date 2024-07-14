package net.hollowcube.mapmaker.map.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.local.config.LocalWorkspace;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.script.engine.ScriptEngine;
import net.hollowcube.mapmaker.map.script.event.ScriptUseItemEvent;
import net.hollowcube.mapmaker.map.script.loader.LocalScriptLoader;
import net.hollowcube.mapmaker.map.world.LocalTestingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@AutoService(FeatureProvider.class)
public class PlayScriptFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("script-events", EventFilter.INSTANCE)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::handlePlayerCleanup)
            .addListener(PlayerUseItemEvent.class, this::handleUseItem)
            .addListener(PlayerUseItemOnBlockEvent.class, this::handleUseItemOnBlock);

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
            var engine = new ScriptEngine(world, new LocalScriptLoader(workspace));
            world.instance().setTag(ScriptEngine.TAG, engine);
            a.complete(null);
        });
        a.join();

        world.instance().eventNode().addChild(eventNode);

        return true;
    }

    @Override
    public void cleanupMap(@NotNull MapWorld world) {
        var engine = world.instance().tagHandler()
                .getAndUpdateTag(ScriptEngine.TAG, _ -> null);
        boolean isTickThread = Thread.currentThread().getName().contains("Tick");
        if (isTickThread) engine.close();
        else {
            CompletableFuture<Void> future = new CompletableFuture<>();
            world.instance().scheduleNextTick(_ -> {
                engine.close();
                future.complete(null);
            });
            FutureUtil.getUnchecked(future);
        }
    }

    private void handlePlayerCleanup(@NotNull MapWorldPlayerStopPlayingEvent event) {
        if (event.getMapWorld().instance().getPlayers().size() == 1) {
            event.getInstance().scheduleNextTick(_ -> ((TestingMapWorld) event.getMapWorld())
                    .buildWorld().closeTestWorld());
        }
    }

    private void handleUseItem(@NotNull PlayerUseItemEvent event) {
        if (event.isCancelled()) return;

        var eventNode = event.getInstance().eventNode();
        eventNode.call(new ScriptUseItemEvent(event.getPlayer()));
    }

    private void handleUseItemOnBlock(@NotNull PlayerUseItemOnBlockEvent event) {
        var eventNode = event.getInstance().eventNode();
        eventNode.call(new ScriptUseItemEvent(event.getPlayer()));
    }

}
