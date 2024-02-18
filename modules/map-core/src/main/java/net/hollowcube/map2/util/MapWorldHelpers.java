package net.hollowcube.map2.util;

import net.hollowcube.map2.MapWorld;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

//import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public final class MapWorldHelpers {
    private MapWorldHelpers() {
    }

    @Blocking
    public static @NotNull SaveState getOrCreateSaveState(
            @NotNull MapWorld world,
            @NotNull String playerId
    ) {
        var mapService = world.server().mapService();
        var map = world.map();

        try {
            return mapService.getLatestSaveState(map.id(), playerId);
        } catch (MapService.NotFoundError ignored) {
            return mapService.createSaveState(map.id(), playerId);
        }
    }

    public static void resetPlayer(@NotNull Player player) {
        player.refreshCommands();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(false);
        player.setFlying(false);
        player.setFlyingSpeed(0.05f);
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);
        player.setInvisible(false);
        player.setVelocity(Vec.ZERO);
        player.clearEffects();
        player.getInventory().clear();
//        player.removeTag(SPECTATOR_CHECKPOINT);

        // Reapply the cosmetics they have on
        var playerData = PlayerDataV2.fromPlayer(player);
        MiscFunctionality.applyCosmetics(player, playerData);

//        if (MapFeatureFlags.DEBUG_PLAYING_OVERLAY.test(player)) {
//            ActionBar.forPlayer(player).addProvider(PlayingDebugOverlay.INSTANCE);
//        }
    }

}
