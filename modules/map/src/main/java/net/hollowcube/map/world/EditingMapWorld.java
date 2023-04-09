package net.hollowcube.map.world;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.gui.hotbar.EditMapHotbar;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.SaveState;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EditingMapWorld extends MapWorld {
    private static final Logger logger = LoggerFactory.getLogger(EditingMapWorld.class);

    private TestingMapWorld testWorld = null;

    public EditingMapWorld(@NotNull MapServer mapServer, @NotNull MapData map) {
        super(mapServer, map);
        flags |= FLAG_EDITING;
    }

    @Override
    protected void initSaveState(@NotNull SaveState saveState) {
        saveState.setEditing(true);
    }

    @Override
    protected void initPlayerFromSaveState(@NotNull Player player, @NotNull SaveState saveState) {
        player.teleport(saveState.getPos()).exceptionally(FutureUtil::handleException);
        player.setGameMode(GameMode.CREATIVE);

        var inventory = saveState.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            player.getInventory().setItemStack(i, inventory.get(i));
        }
        if (saveState.getNbt() != null) {
            player.tagHandler().updateContent(saveState.getNbt());
        }

        player.sendMessage("Now editing " + map.getName());
    }

    @Override
    protected void initHotbar(@NotNull Player player) {
        EditMapHotbar.applyToPlayer(player);
    }

    @Override
    protected void updateSaveStateForPlayer(@NotNull Player player, @NotNull SaveState saveState, boolean remove) {
        saveState.setPos(player.getPosition());
        saveState.setNbt(player.tagHandler().asCompound());
        saveState.setInventory(List.of(player.getInventory().getItemStacks()));
    }

    @Override
    protected @NotNull FutureResult<Void> closeWorld() {
        return FutureResult.wrap(saveAndUnloadWorld())
                //todo not sure how to handle this better.
                .thenErr(err -> logger.error("Failed to save and unload world: {}", err));
    }
}
