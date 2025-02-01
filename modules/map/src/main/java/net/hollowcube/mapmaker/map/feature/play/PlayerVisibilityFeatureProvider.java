package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.VisibilityRule;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerStartFinishedEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerStartSpectatorEvent;
import net.hollowcube.mapmaker.map.event.trait.MapWorldEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.util.PlayerVisibilityExtension;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

@AutoService(FeatureProvider.class)
public class PlayerVisibilityFeatureProvider implements FeatureProvider {

    private static final double PLAYER_HIDE_DISTANCE_SQR = 5.0 * 5.0;
    private static final double PLAYER_HIDE_DISTANCE_TO_POI_SQR = 3.5 * 3.5;
    private static final double SPECTATOR_HIDE_DISTANCE_SQR = PLAYER_HIDE_DISTANCE_SQR * 2.0;

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/visibility/events", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::onPlayerInit)
            .addListener(MapPlayerStartSpectatorEvent.class, this::onPlayerExitingPlay)
            .addListener(MapPlayerStartFinishedEvent.class, this::onPlayerExitingPlay);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld)) return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR) return false;

        world.eventNode().addChild(eventNode);

        world.instance().scheduler()
                .buildTask(() -> tickViewers(world))
                .repeat(TaskSchedule.tick(5))
                .schedule();

        return true;
    }

    private void onPlayerInit(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        var world = event.getMapWorld();
        if (!world.isPlaying(player)) return;

        var data = PlayerDataV2.fromPlayer(player);

        player.scheduleNextTick($ -> {
            player.updateViewerRule(new PlayerViewerRule(player, world, data));
            if (player instanceof PlayerVisibilityExtension extension) {
                extension.setVisibilityFunc(new PlayerVisibilityRule(player, world, data));
            }
        });
    }

    private <T extends MapWorldEvent & PlayerEvent> void onPlayerExitingPlay(@NotNull T event) {
        var player = event.getPlayer();
        var world = event.getMapWorld();
        var data = PlayerDataV2.fromPlayer(player);

        player.scheduleNextTick($ -> {
            player.updateViewerRule(null);
            if (player instanceof PlayerVisibilityExtension extension) {
                extension.setVisibilityFunc(new PlayerVisibilityRule(player, world, data));
            }
        });
    }

    private static void tickViewers(@NotNull MapWorld world) {
        for (Player player : Set.copyOf(world.players())) {
            player.updateViewerRule(); // Only players have special viewable rules
            if (player instanceof PlayerVisibilityExtension extension) {
                extension.updateVisibility();
            }
        }
        for (Player spectator : Set.copyOf(world.spectators())) {
            if (spectator instanceof PlayerVisibilityExtension extension) {
                extension.updateVisibility();
            }
        }
    }

    private record PlayerViewerRule(Player self, MapWorld world, PlayerDataV2 data) implements Predicate<Entity> {

        @Override
        public boolean test(Entity entity) {
            if (!(entity instanceof Player other)) return true; // Allow non-players
            if (isPlayerPlaying(world, other)) {
                return data.getSetting(PlayerSettings.NEARBY_PLAYER_VISIBILITY) == VisibilityRule.GHOST;
            } else if (world.isSpectating(other)) {
                return self.getDistanceSquared(other) > SPECTATOR_HIDE_DISTANCE_SQR;
            }
            return true;
        }
    }

    private record PlayerVisibilityRule(Player self, MapWorld world, PlayerDataV2 data) implements Function<Player, PlayerVisibilityExtension.Visibility> {

        // Check if a viewer can see self
        @Override
        public PlayerVisibilityExtension.Visibility apply(Player viewer) {
            if (isPlayerPlaying(this.world, this.self)) {
                var ourPos = this.self.getPosition();
                if (viewer.getDistanceSquared(this.self) <= PLAYER_HIDE_DISTANCE_SQR) {
                    return PlayerVisibilityExtension.Visibility.INVISIBLE;
                } else if (this.world.spawnPoint(this.self).distanceSquared(ourPos) <= PLAYER_HIDE_DISTANCE_TO_POI_SQR) {
                    return PlayerVisibilityExtension.Visibility.INVISIBLE;
                } else {
                    var selfCheckpoint = OpUtils.map(SaveState.optionalFromPlayer(this.self), state -> state.state(PlayState.class).pos().orElse(null));
                    var otherCheckpoint = OpUtils.map(SaveState.optionalFromPlayer(viewer), state -> state.state(PlayState.class).pos().orElse(null));

                    if (selfCheckpoint != null && selfCheckpoint.distanceSquared(ourPos) <= PLAYER_HIDE_DISTANCE_TO_POI_SQR) {
                        return PlayerVisibilityExtension.Visibility.INVISIBLE;
                    } else if (otherCheckpoint != null && otherCheckpoint.distanceSquared(ourPos) <= PLAYER_HIDE_DISTANCE_TO_POI_SQR) {
                        return PlayerVisibilityExtension.Visibility.INVISIBLE;
                    }
                }
            } else if (this.world.isSpectating(this.self)) {
                return PlayerVisibilityExtension.Visibility.SPECTATOR;
            }
            return PlayerVisibilityExtension.Visibility.VISIBLE;
        }
    }

    private static boolean isPlayerPlaying(MapWorld world, Player player) {
        world = world instanceof TestingMapWorld testingWorld ? testingWorld.buildWorld() : world;
        return world.isPlaying(player);
    }
}
