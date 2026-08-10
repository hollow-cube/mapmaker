/// The parkour-specific replay events, appended to the generic ones in
/// [net.hollowcube.mapmaker.runtime.parkour.replay.ReplayManager#REGISTRY].
///
/// Nearly every one of these is a moment in a run rather than a description of it, so playback is
/// free to ignore them; the exception is
/// [net.hollowcube.mapmaker.runtime.parkour.replay.event.ClearGhostBlocksEvent], which
/// [net.hollowcube.mapmaker.runtime.parkour.replay.ParkourPlaybackHandler] applies.
@NotNullByDefault
package net.hollowcube.mapmaker.runtime.parkour.replay.event;

import org.jetbrains.annotations.NotNullByDefault;
