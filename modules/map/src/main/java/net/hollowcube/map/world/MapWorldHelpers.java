package net.hollowcube.map.world;

import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.map.MapFeatureFlags;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.util.debug.PlayingDebugOverlay;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

class MapWorldHelpers {
    private MapWorldHelpers() {
    }

    public static @NotNull List<FeatureProvider> loadFeatures(@NotNull InternalMapWorld world) {
        var enabledFeatures = new ArrayList<FeatureProvider>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Load each feature in parallel
            var features = world.server().features();
            var enabledFutures = new Future[features.size()];
            for (int i = 0; i < features.size(); i++) {
                var feature = features.get(i);
                enabledFutures[i] = scope.fork(() -> {
                    try {
                        return feature.initMap(world);
                    } catch (Exception e) {
                        MinecraftServer.getExceptionManager().handleException(new RuntimeException(
                                "failed to load feature " + feature.getClass().getName(), e));
                        return false;
                    }
                });
            }

            scope.join();

            // Add each feature to the enabled list if it is enabled.
            for (int i = 0; i < features.size(); i++) {
                var feature = features.get(i);
                if ((boolean) enabledFutures[i].resultNow()) {
                    enabledFeatures.add(feature);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return enabledFeatures;
    }

    @Blocking
    public static @NotNull SaveState getOrCreateSaveState(
            @NotNull InternalMapWorld world,
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
        player.removeTag(SPECTATOR_CHECKPOINT);

        // Reapply the cosmetics they have on
        var playerData = PlayerDataV2.fromPlayer(player);
        MiscFunctionality.applyCosmetics(player, playerData);

        if (MapFeatureFlags.DEBUG_PLAYING_OVERLAY.test(player)) {
            ActionBar.forPlayer(player).addProvider(PlayingDebugOverlay.INSTANCE);
        }
    }

}
