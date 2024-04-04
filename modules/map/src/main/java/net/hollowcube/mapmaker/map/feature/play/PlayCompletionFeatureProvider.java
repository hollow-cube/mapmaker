package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.cosmetic.impl.AbstractVictoryEffectImpl;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.gui.RateMapView;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.player.AppliedRewards;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.Future;

import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;

@AutoService(FeatureProvider.class)
public class PlayCompletionFeatureProvider implements FeatureProvider {

    private static final Tag<Future<SaveState>> BEST_SAVE_STATE_TAG = Tag.Transient("map:best_save_state");

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld))
            return false;

        var eventNode = EventNode.type("map-completion/play", EventFilter.INSTANCE);
        eventNode.addListener(MapPlayerInitEvent.class, this::handlePlayerInit);
        eventNode.addListener(MapWorldPlayerStopPlayingEvent.class, this::handlePlayerRemove);
        eventNode.addListener(MapPlayerCompleteMapEvent.class, this::handleMapCompletion);
        world.eventNode().addChild(eventNode);

        return true;
    }

    private void handlePlayerInit(@NotNull MapPlayerInitEvent event) {
        if (!event.isFirstInit()) return;

        var player = event.getPlayer();
        var world = event.getMapWorld();

        var future = MapService.VIRTUAL_EXECUTOR.submit(() -> world.server().mapService()
                .getBestSaveState(world.map().id(), player.getUuid().toString()));
        player.setTag(BEST_SAVE_STATE_TAG, future);
    }

    private void handlePlayerRemove(@NotNull MapWorldPlayerStopPlayingEvent event) {
        event.getPlayer().removeTag(BEST_SAVE_STATE_TAG);
    }

    private void handleMapCompletion(@NotNull MapPlayerCompleteMapEvent event) {
        var player = event.getPlayer();
        var world = (PlayingMapWorld) event.getMapWorld(); // Safe because this is only enabled on playing worlds.

        var saveState = SaveState.fromPlayer(player);
        saveState.setCompleted(true); // Also stops recording time here
        var finishFuture = player.getTag(BEST_SAVE_STATE_TAG); // Fetch now because it will be removed when player is removed from world.

        // Pre-remove the playing tags
        //todo this is a bad solution. Basically we need to remove the player immediately, but the remove method runs in a virtual thread
        // which means it will have a tiny scheduling delay which means duplicates can trigger. To get around this we just remove these
        // two tags immediately which will stop them from triggering new events. Its a terrible solution and needs to be reworked.
        world.removePlayerImmediate(player);

        FutureUtil.submitVirtual(() -> {
            // Remove the player from the world itself, they are no longer playing (but will remain in the instance)
            // This will also cause their savestate to be written to DB
            // Then re-add the player to the world as a spectator (in finished mode)

            var resp = world.removeActivePlayer(player);
            world.addSpectator(player, true);

            // Show the completed message after removing the player because it is theoretically possible to not have the savestate fetched yet.
            var bestSaveState = FutureUtil.getUnchecked(finishFuture);
            if (bestSaveState == null) {
                player.sendMessage(Component.translatable("map.completed.first", Component.text(formatMapPlaytime(saveState.getPlaytime(), true))));
            } else {
                // Diff playtime rounded to ticks prior to subtracting for correct display.
                var diffPlaytime = (bestSaveState.getPlaytime() - bestSaveState.getPlaytime() % MinecraftServer.TICK_MS) - (saveState.getPlaytime() - saveState.getPlaytime() % MinecraftServer.TICK_MS);
                player.sendMessage(Component.translatable("map.completed.with_prior",
                        Component.text(formatMapPlaytime(saveState.getPlaytime(), true)),
                        // Note: roundToTicks is not used here. We do the rounding above because we need to round prior to calculating the difference.
                        Component.text((diffPlaytime < 0 ? "+" : "-") + formatMapPlaytime(Math.abs(diffPlaytime), false), diffPlaytime < 0 ? NamedTextColor.RED : NamedTextColor.GREEN)));
            }

            // Will be called when the completion animation is finished
            var lastRatingFuture = player.getTag(MapRatingFeatureProvider.LAST_RATING_TAG);
            Runnable tryShowRateGui = () -> {
                if (MapRatingFeatureProvider.isMapRatable(world)) {
                    var lastRating = FutureUtil.getUnchecked(lastRatingFuture);
                    if (lastRating == null || lastRating.state() == MapRating.State.UNRATED) {
                        world.server().showView(player, c -> new RateMapView(c, world.map()));
                    }
                }
            };

            var rewards = Optional.ofNullable(resp)
                    .map(SaveStateUpdateResponse::rewards);

            // Apply the rewards
            rewards.map(AppliedRewards::newState).ifPresent(newState -> {
                var playerData = PlayerDataV2.fromPlayer(player);
                if (newState.hasCoins()) //noinspection DataFlowIssue
                    playerData.setCoins(newState.coins());
                if (newState.hasExp()) //noinspection DataFlowIssue
                    playerData.setExperience(newState.exp());
                if (newState.hasCubits()) //noinspection DataFlowIssue
                    playerData.setCubits(newState.cubits());
            });

            // Show the completion animation
            rewards.map(AppliedRewards::diff)
                    .ifPresent(diff -> MapCompletionAnimation.schedule(player, diff, tryShowRateGui));

            // Play the victory effect
            var playerData = PlayerDataV2.fromPlayer(player);
            var victoryEffect = Cosmetic.byId(CosmeticType.VICTORY_EFFECT, playerData.getSetting(CosmeticType.VICTORY_EFFECT.setting()));
            if (victoryEffect != null && victoryEffect.impl() instanceof AbstractVictoryEffectImpl impl) {
                impl.trigger(player, player.getPosition());
            }
        });
    }

}
