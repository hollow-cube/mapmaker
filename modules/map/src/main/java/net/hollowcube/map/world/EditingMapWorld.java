package net.hollowcube.map.world;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.MapServer;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.util.FutureUtil;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingMapWorld extends MapWorld {
    private static final Logger logger = LoggerFactory.getLogger(EditingMapWorld.class);

    public EditingMapWorld(@NotNull MapServer mapServer, @NotNull MapData map) {
        super(mapServer, map);
    }

    @Override
    protected @NotNull FutureResult<Void> initPlayer(@NotNull Player player) {
        player.teleport(map.getSpawnPoint()).exceptionally(FutureUtil::handleException);
        player.setGameMode(GameMode.CREATIVE);

        player.sendMessage("Now editing " + map.getName());
        return FutureResult.ofNull();
    }

    @Override
    protected @NotNull FutureResult<Void> savePlayer(@NotNull Player player, boolean remove) {
        //todo
        return FutureResult.ofNull();
    }

    @Override
    protected @NotNull FutureResult<Void> closeWorld() {
        return FutureResult.wrap(saveAndUnloadWorld())
                //todo not sure how to handle this better.
                .thenErr(err -> logger.error("Failed to save and unload world: {}", err));
    }
}
