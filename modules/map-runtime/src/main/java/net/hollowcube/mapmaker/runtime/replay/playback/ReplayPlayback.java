package net.hollowcube.mapmaker.runtime.replay.playback;

import dev.hollowcube.replay.ReplayPlayer;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayEventRegistry;
import dev.hollowcube.replay.io.CompactedReplayReader;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/// Plays a replay into a world, with the timing controls a viewer expects.
///
/// Loading is deliberately not part of this class: a [CompactedReplaySource] blocks, so a host
/// loads the bytes off the tick thread and constructs this with them once it is back on it.
///
/// Like [ReplayPlayer] this owns no scheduler; [#tick()] is called once per server tick by whoever
/// is driving playback, which is what makes speed mean anything.
public final class ReplayPlayback implements AutoCloseable {
    private final ReplayPlayer player;
    private final @Nullable ReplayScene scene;

    private boolean playing;
    private boolean looping;
    private double speed = 1;

    /// Replay ticks owed but not yet played, so speeds below 1 can span server ticks.
    private double budget;
    private boolean buffering;

    /// `handler`, if given, sees every event played after the scene has applied it.
    ///
    /// The scene only knows the generic events, so this is how a host observes the ones it added to
    /// its own registry, without teaching playback what they mean. It is passed in rather than
    /// built here because a scene is worth nothing until a host has given it viewers, and closing
    /// this closes it either way.
    public ReplayPlayback(
        byte[] replay, ReplayEventRegistry registry,
        ReplayScene scene,
        @Nullable Consumer<ReplayEvent> handler
    ) {
        this.scene = scene;
        this.player = new ReplayPlayer(
            new CompactedReplayReader(replay), registry,
            handler == null ? scene : scene.andThen(handler)
        );
    }

    /// Drives a player without a world, for tests of the timing itself.
    ReplayPlayback(ReplayPlayer player) {
        this.player = player;
        this.scene = null;
    }

    /// Plays whatever replay ticks this server tick is owed. Does nothing while paused.
    public void tick() {
        if (!playing) return;

        budget += speed;
        while (budget >= 1) {
            budget -= 1;
            buffering = false;
            switch (player.advance()) {
                case ADVANCED -> {
                }
                case STALLED -> {
                    stall();
                    return;
                }
                case FINISHED -> {
                    if (!looping) {
                        playing = false;
                        budget = 0;
                        return;
                    }

                    // Looping leaves the scene as the last tick left it; nothing removes entities
                    // the recording never destroyed, and the first keyframes put the rest back.
                    player.seek(0);
                    if (player.advance() != ReplayPlayer.Advance.ADVANCED) {
                        stall();
                        return;
                    }
                }
            }
        }
    }

    /// True while playback is waiting on replay data it does not have yet.
    public boolean buffering() {
        return buffering;
    }

    public boolean playing() {
        return playing;
    }

    public void play() {
        playing = true;
    }

    public void pause() {
        playing = false;
    }

    public double speed() {
        return speed;
    }

    public void setSpeed(double speed) {
        if (speed <= 0) throw new IllegalArgumentException("speed must be positive");
        this.speed = speed;
    }

    public boolean looping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    /// The tick the next [#tick()] will play.
    public int currentTick() {
        return player.tick();
    }

    public int tickCount() {
        return player.tickCount();
    }

    public boolean finished() {
        return player.tick() >= player.tickCount();
    }

    /// Jumps to a tick, dropping any partially accumulated budget so the new position plays whole.
    public void seek(int tick) {
        player.seek(tick);
        budget = 0;
    }

    /// Drops the accumulated budget along with the ticks it owed. Banking them would make the
    /// replay fast forward through everything it missed the moment the data lands, which reads far
    /// worse than the stall itself did.
    private void stall() {
        buffering = true;
        budget = 0;
    }

    @Override
    public void close() {
        playing = false;
        if (scene != null) scene.close();
        player.close();
    }
}
