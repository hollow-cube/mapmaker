package net.hollowcube.mapmaker.anticheat;

import net.hollowcube.anticheat.log.TraceHeader;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnticheatConfigTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
    private static final UUID TRUSTED = UUID.fromString("00000000-0000-0000-0000-0000000005af");

    @Test
    void nothingIsCapturedWhileTheFeatureIsOff() {
        var config = new AnticheatConfig(false, 1, TRUSTED.toString(), false, 600);

        assertFalse(config.shouldCapture(PLAYER, true));
        assertFalse(config.shouldCapture(TRUSTED, true));
    }

    @Test
    void aRateOfZeroCapturesNobodyAndOneCapturesEverybody() {
        assertFalse(new AnticheatConfig(true, 0, "", false, 600).shouldCapture(PLAYER, true));
        assertTrue(new AnticheatConfig(true, 1, "", false, 600).shouldCapture(PLAYER, true));
    }

    @Test
    void theTrustedCohortIsCapturedWhateverTheRate() {
        var config = new AnticheatConfig(true, 0, " " + TRUSTED + " ,", false, 600);

        assertTrue(config.shouldCapture(TRUSTED, true));
        assertFalse(config.shouldCapture(PLAYER, true));
        assertEquals(TraceHeader.Cohort.TRUSTED, config.cohort(TRUSTED));
        assertEquals(TraceHeader.Cohort.RANDOM, config.cohort(PLAYER));
    }

    @Test
    void playingOnlyRefusesAPlayerWhoIsNotPlaying() {
        var playingOnly = new AnticheatConfig(true, 1, TRUSTED.toString(), true, 600);

        assertTrue(playingOnly.shouldCapture(PLAYER, true));
        assertFalse(playingOnly.shouldCapture(PLAYER, false));
        // Not even the trusted cohort is worth a trace of somebody spectating.
        assertFalse(playingOnly.shouldCapture(TRUSTED, false));

        assertTrue(new AnticheatConfig(true, 1, "", false, 600).shouldCapture(PLAYER, false));
    }

    @Test
    void aBadRateOrCapIsCorrectedRatherThanCarried() {
        assertEquals(1, new AnticheatConfig(true, 4, "", true, 600).rate());
        assertEquals(0, new AnticheatConfig(true, -1, "", true, 600).rate());
        assertEquals(AnticheatConfig.DEFAULT_MAX_SECONDS, new AnticheatConfig(true, 1, "", true, 0).maxSeconds());
    }
}
