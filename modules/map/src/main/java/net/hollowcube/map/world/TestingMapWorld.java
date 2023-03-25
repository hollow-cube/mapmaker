package net.hollowcube.map.world;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.feature.CheckpointFeature;
import net.hollowcube.map.gui.hotbar.PlayingMapHotbar;
import net.hollowcube.map.gui.hotbar.TestingMapHotbar;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO PlayingMapWorld and TestingMapWorld can/should extend a base "immutable" map world
public class TestingMapWorld extends MapWorld {
    private static final Logger logger = LoggerFactory.getLogger(TestingMapWorld.class);

    public TestingMapWorld(@NotNull MapServer mapServer, @NotNull MapData map) {
        super(mapServer, map);
        var eventNode = instance().eventNode();
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak); //todo again, BaseWorld settings
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace); //todo again, BaseWorld settings

        eventNode.addChild(new CheckpointFeature().eventNode()); //todo auto registration with selector
        eventNode.addChild(TestingMapHotbar.eventNode());
    }

    @Override
    protected void initHotbar(@NotNull Player player) {
        TestingMapHotbar.applyToPlayer(player);
    }

    @Override
    protected @NotNull FutureResult<Void> closeWorld() {
        return FutureResult.wrap(unloadWorld())
                //todo handle error better.
                .thenErr(err -> logger.error("Failed to unload world: {}", err));
    }

    private void preventBlockBreak(PlayerBlockBreakEvent event) {
        event.setCancelled(true);
    }

    private void preventBlockPlace(PlayerBlockPlaceEvent event) {
        event.setCancelled(true);
    }
}
