package net.hollowcube.mapmaker.runtime.parkour.replay;

import dev.hollowcube.replay.io.SegmentedReplay;
import dev.hollowcube.replay.io.SegmentedReplayStorage;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class ReplayManagerTest {

    @BeforeEach
    void enableRecording() {
        recordingEnabled(true);
    }

    @AfterEach
    void resetFeatureFlags() {
        recordingEnabled(false);
    }

    @Test
    void aRecordingStorageCannotReadDisablesRecordingRatherThanFailing() {
        var saveState = saveState();
        var recordings = recordings(() -> {
            throw new IllegalArgumentException("replay preamble header contains negative lengths: metadata=-1");
        });

        // Preparation runs during player configuration, so anything thrown here denies the join.
        assertDoesNotThrow(() -> recordings.prepare(saveState, playerData()));
        assertFalse(recordings.recordable(saveState.id()));
        assertNull(recordings.take(saveState.id()));
    }

    @Test
    void aFinishedRecordingIsNotRecordableAndAnAbsentOneIs() {
        var saveState = saveState();

        var finished = recordings(() -> new SegmentedReplay(null, null, true));
        finished.prepare(saveState, playerData());
        assertFalse(finished.recordable(saveState.id()));

        var absent = recordings(() -> null);
        absent.prepare(saveState, playerData());
        assertTrue(absent.recordable(saveState.id()));
        assertNull(absent.take(saveState.id()));
    }

    @Test
    void recordingDisabledForThePlayerNeverReadsStorage() {
        recordingEnabled(false);
        var saveState = saveState();
        var recordings = recordings(() -> fail("a disabled recording must not be loaded"));

        recordings.prepare(saveState, playerData());
        assertFalse(recordings.recordable(saveState.id()));
        assertNull(recordings.take(saveState.id()));
    }

    @Test
    void aRecordingDisabledMidRunStaysBlockedUntilItIsPreparedAgain() {
        var saveState = saveState();
        var recordings = recordings(() -> null);
        recordings.prepare(saveState, playerData());
        assertTrue(recordings.recordable(saveState.id()));

        // What a tick does when recording is switched off underneath a live run.
        recordings.disable(saveState.id());
        assertFalse(recordings.recordable(saveState.id()));

        // Only preparation finds the recording to resume, so recording restarts no earlier.
        recordings.prepare(saveState, playerData());
        assertTrue(recordings.recordable(saveState.id()));
    }

    private static void recordingEnabled(boolean enabled) {
        FeatureFlagProvider.replaceGlobals((_, _) -> enabled);
    }

    private static PlayerData playerData() {
        return new PlayerData(UUID.randomUUID().toString(), "tester", Component.text("tester"));
    }

    private static ReplayManager.PreparedRecordings recordings(Supplier<@Nullable SegmentedReplay> load) {
        return new ReplayManager.PreparedRecordings(new SegmentedReplayStorage() {
            @Override
            public @Nullable SegmentedReplay load(String replayId) {
                return load.get();
            }

            @Override
            public SegmentedReplayWriter writer(String replayId, @Nullable SegmentedReplay base) {
                throw new AssertionError("nothing should be written");
            }
        }, UUID.randomUUID());
    }

    private static SaveState saveState() {
        return new SaveState(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), SaveStateType.PLAYING, PlayState.SERIALIZER, new PlayState());
    }
}
