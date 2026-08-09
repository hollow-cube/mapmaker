package net.hollowcube.mapmaker.runtime.parkour.replay;

import dev.hollowcube.replay.event.ReplayEvent;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.ClearGhostBlocksEvent;
import net.hollowcube.mapmaker.runtime.replay.playback.ReplayScene;

import java.util.function.Consumer;

/// Applies the parkour events that change what a scene looks like.
///
/// Only [ClearGhostBlocksEvent] does. The rest are moments in a run, which a viewer draws on top of a
/// scene rather than something the scene itself has to know about.
public final class ParkourPlaybackHandler implements Consumer<ReplayEvent> {
    private final ReplayScene scene;

    public ParkourPlaybackHandler(ReplayScene scene) {
        this.scene = scene;
    }

    @Override
    public void accept(ReplayEvent event) {
        if (event instanceof ClearGhostBlocksEvent) scene.restoreBlocks();
    }
}
