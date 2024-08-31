package net.hollowcube.mapmaker.map.util;

import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MapWorldHelpers {
    private MapWorldHelpers() {
    }

    @Blocking
    public static @NotNull SaveState getOrCreateSaveState(
            @NotNull MapWorld world,
            @NotNull String playerId,
            @NotNull SaveStateType type,
            @Nullable SaveStateType.Serializer<?> stateSerializer
    ) {
        var mapService = world.server().mapService();
        var map = world.map();

        try {
            return mapService.getLatestSaveState(map.id(), playerId, type, stateSerializer);
        } catch (MapService.NotFoundError ignored) {
            return mapService.createSaveState(map.id(), playerId, stateSerializer);
        }
    }

    public static void resetPlayer(@NotNull Player player) {
        player.refreshCommands();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(false);
        player.setFlying(false);
        player.setFlyingSpeed(0.05f);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);
        player.setInvisible(false);
        player.setVelocity(Vec.ZERO);
        player.clearEffects();
        player.getInventory().clear();
        player.updateViewerRule(null);

        // Reapply the cosmetics they have on
        var playerData = PlayerDataV2.fromPlayer(player);
        MiscFunctionality.applyCosmetics(player, playerData);
    }

}
