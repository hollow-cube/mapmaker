package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.io.Dispatch;
import net.hollowcube.mapmaker.runtime.io.PlayerInput;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.RespawnPosAction;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.CheckpointReachedEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.kyori.adventure.text.Component.translatable;

/// Set a checkpoint.
///
/// @param temporary true for a spectator checkpoint
public record CheckpointPlayerInput(
    @Nullable RespawnPosAction respawn,
    boolean temporary
) implements PlayerInput {
    public static final Key KEY = Key.key("mapmaker", "checkpoint");

    private static final Sound CHECKPOINT_SOUND = Sound.sound(
        SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP,
        Sound.Source.MASTER,
        0.1f,
        0f
    );

    @Override
    public void deliver(Dispatch.Delivery delivery) {
        var player = delivery.player();
        var playState = delivery.state();
        var source = delivery.source();
        var world = Objects.requireNonNull(ParkourMapWorld.forPlayer(player));

        var respawnPosition = OpUtils.mapOr(
            respawn,
            action -> {
                float yaw = player.getPosition().yaw();
                float pitch = player.getPosition().pitch();
                var origin = source.position() instanceof BlockVec(int x, int y, int z)
                    ? new Pos(x + 0.5, y, z + 0.5, yaw, pitch)
                    : source.position().asPos(yaw, pitch);
                return action.target().resolve(origin);
            },
            player.getPosition()
        );

        List<String> newHistory;
        if (MapSettings.get(world.map().settings(), MapSettings.PROGRESS_INDEX_ADDITION)) {
            // With additive progress index you can never touch a previous checkpoint
            playState.history().add(source.id());
            newHistory = List.copyOf(playState.history());
        } else {
            // Only this checkpoint, so earlier ones can be reached again unless progress indices prevent it
            newHistory = List.of(source.id());
        }
        playState.setLastState(
            new PlayState(
                null,
                newHistory,
                respawnPosition,
                Map.copyOf(playState.ghostBlocks()),
                Map.copyOf(playState.actionData())
            )
        );

        if (!temporary && world.getPlayerState(player) instanceof ParkourState.AnyPlaying p) {
            world.recordReplayEvent(
                player,
                new CheckpointReachedEvent(source.id(), p.saveState().getRealPlaytime())
            );
        }
        player.sendMessage(
            translatable(temporary ? "spec.checkpoint.set" : "play.checkpoint.reached")
        );
        player.playSound(CHECKPOINT_SOUND);
    }
}
