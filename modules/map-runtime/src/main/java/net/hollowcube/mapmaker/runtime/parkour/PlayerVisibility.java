package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

public final class PlayerVisibility {
    private static final double PLAYER_HIDE_DISTANCE_SQR = 5.0 * 5.0;
    private static final double PLAYER_HIDE_DISTANCE_TO_POI_SQR = 3.5 * 3.5;
    private static final double SPECTATOR_HIDE_DISTANCE_SQR = PLAYER_HIDE_DISTANCE_SQR * 2.0;

    private PlayerVisibility() {
    }

    public record ViewerRule(
            Player self, ParkourMapWorld world, PlayerData data
    ) implements Predicate<Entity> {
        public ViewerRule(Player self, ParkourMapWorld world) {
            this(self, world, PlayerData.fromPlayer(self));
        }

        @Override
        public boolean test(Entity entity) {
            if (!(entity instanceof Player other)) return true; // Allow non-players
            return switch (world.getPlayerState(other)) {
                case ParkourState.AnyPlaying _ ->
                        data.getSetting(PlayerSettings.NEARBY_PLAYER_VISIBILITY) == net.hollowcube.mapmaker.map.VisibilityRule.GHOST;
                case ParkourState.Spectating _, ParkourState.Finished _ ->
                        self.getDistanceSquared(other) > SPECTATOR_HIDE_DISTANCE_SQR;
                case null -> true;
            };
        }

    }

    public record VisibilityRule(
            Player self, ParkourMapWorld world2, PlayerData data
    ) implements Function<Player, net.hollowcube.mapmaker.map.util.PlayerVisibility> {
        public VisibilityRule(Player self, ParkourMapWorld world2) {
            this(self, world2, PlayerData.fromPlayer(self));
        }

        private @Nullable Pos getCheckpointPos(Player player) {
            return switch (world2.getPlayerState(player)) {
                case ParkourState.AnyPlaying p -> p.saveState().state(PlayState.class).pos();
                case null, default -> null;
            };
        }

        // Check if a viewer can see self
        @Override
        public net.hollowcube.mapmaker.map.util.PlayerVisibility apply(Player viewer) {
            return switch (this.world2.getPlayerState(this.self)) {
                case ParkourState.AnyPlaying _ -> {
                    var ourPos = this.self.getPosition();
                    if (viewer.getDistanceSquared(this.self) <= PLAYER_HIDE_DISTANCE_SQR) {
                        yield net.hollowcube.mapmaker.map.util.PlayerVisibility.INVISIBLE;
                    } else if (this.world2.map().settings().getSpawnPoint().distanceSquared(ourPos) <= PLAYER_HIDE_DISTANCE_TO_POI_SQR) {
                        yield net.hollowcube.mapmaker.map.util.PlayerVisibility.INVISIBLE;
                    } else {
                        var selfCheckpoint = getCheckpointPos(this.self);
                        var otherCheckpoint = getCheckpointPos(viewer);

                        if (selfCheckpoint != null && selfCheckpoint.distanceSquared(ourPos) <= PLAYER_HIDE_DISTANCE_TO_POI_SQR) {
                            yield net.hollowcube.mapmaker.map.util.PlayerVisibility.INVISIBLE;
                        } else if (otherCheckpoint != null && otherCheckpoint.distanceSquared(ourPos) <= PLAYER_HIDE_DISTANCE_TO_POI_SQR) {
                            yield net.hollowcube.mapmaker.map.util.PlayerVisibility.INVISIBLE;
                        }
                    }
                    yield net.hollowcube.mapmaker.map.util.PlayerVisibility.VISIBLE;
                }
                case ParkourState.Spectating _, ParkourState.Finished _ ->
                        net.hollowcube.mapmaker.map.util.PlayerVisibility.SPECTATOR;
                case null -> net.hollowcube.mapmaker.map.util.PlayerVisibility.VISIBLE;
            };
        }
    }
}