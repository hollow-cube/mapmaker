package net.hollowcube.map.world;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.gui.hotbar.TestingMapHotbar;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO PlayingMapWorld and TestingMapWorld can/should extend a base "immutable" map world
public class TestingMapWorld {
    private static final System.Logger logger = System.getLogger(TestingMapWorld.class.getName());

    public TestingMapWorld(@NotNull Instance editInstance) {

//        var eventNode = instance().eventNode();
//        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak); //todo again, BaseWorld settings
//        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace); //todo again, BaseWorld settings
//
//        eventNode.addChild(TestingMapHotbar.eventNode());
    }

//    @Override
//    protected void initHotbar(@NotNull Player player) {
//        TestingMapHotbar.applyToPlayer(player);
//    }
//
//    @Override
//    protected @NotNull FutureResult<Void> closeWorld() {
//        return FutureResult.wrap(unloadWorld())
//                .thenErr(err -> logger.log(System.Logger.Level.ERROR, "Failed to unload world: {0}", err));
//    }
//
//    private void preventBlockBreak(PlayerBlockBreakEvent event) {
//        event.setCancelled(true);
//    }
//
//    private void preventBlockPlace(PlayerBlockPlaceEvent event) {
//        event.setCancelled(true);
//    }
}
