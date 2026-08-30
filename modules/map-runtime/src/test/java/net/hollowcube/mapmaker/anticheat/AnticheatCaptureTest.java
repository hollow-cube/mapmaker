package net.hollowcube.mapmaker.anticheat;

import net.hollowcube.anticheat.capture.TrimPolicy;
import net.hollowcube.anticheat.control.CaptureControl;
import net.hollowcube.anticheat.log.TraceHeader;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class AnticheatCaptureTest {

    /// The guard the run hooks lean on: a run that was never sampled must not close a capture that
    /// was never opened, and the one that was opened must not be closed twice.
    @Test
    void aStopIsOnlySentForACaptureThatWasStarted(Env env) {
        var instance = env.createFlatInstance();
        var connection = env.createConnection();
        var player = connection.connect(instance, new Pos(0, 40, 0));
        // A collector stops collecting once read, so the two halves of this get one each.
        var beforeStart = connection.trackIncoming(PluginMessagePacket.class);
        AnticheatCapture.stop(player, "run-1");
        assertEquals(List.of(), controlMessages(beforeStart));
        assertNull(AnticheatCapture.activeCapture(player));

        var packets = connection.trackIncoming(PluginMessagePacket.class);
        AnticheatCapture.start(player, "run-1", TraceHeader.Reason.RUN, TraceHeader.Cohort.RANDOM,
            TrimPolicy.DEFAULT);
        assertEquals("run-1", AnticheatCapture.activeCapture(player));

        AnticheatCapture.stop(player, "run-2"); // A capture that is not the open one.
        AnticheatCapture.stop(player, "run-1");
        AnticheatCapture.stop(player, "run-1"); // The same one twice, as a disconnect after a finish would.

        assertEquals(List.of(
            new CaptureControl.Start("run-1", TraceHeader.Reason.RUN, TraceHeader.Cohort.RANDOM,
                TrimPolicy.DEFAULT),
            new CaptureControl.Stop("run-1")), controlMessages(packets));
        assertNull(AnticheatCapture.activeCapture(player));
    }

    @Test
    void aFlushCarriesWhicheverCaptureIsOpen(Env env) {
        var instance = env.createFlatInstance();
        var connection = env.createConnection();
        var player = connection.connect(instance, new Pos(0, 40, 0));
        var packets = connection.trackIncoming(PluginMessagePacket.class);

        AnticheatCapture.flush(player, TraceHeader.Reason.MANUAL);
        AnticheatCapture.start(player, "run-1", TraceHeader.Reason.RUN, null, TrimPolicy.DEFAULT);
        AnticheatCapture.flush(player, TraceHeader.Reason.MANUAL);

        assertEquals(List.of(
            new CaptureControl.Flush(null, TraceHeader.Reason.MANUAL),
            new CaptureControl.Start("run-1", TraceHeader.Reason.RUN, null, TrimPolicy.DEFAULT),
            new CaptureControl.Flush("run-1", TraceHeader.Reason.MANUAL)), controlMessages(packets));
    }

    /// The flag payload is hand-typed in the posthog UI, so both what it should look like and what
    /// a typo does are behavior worth pinning.
    @Test
    void aFlagPayloadParsesIntoAnEnabledConfig() {
        var config = AnticheatCapture.parse(
            "{\"rate\":0.25,\"trusted\":\"00000000-0000-0000-0000-0000000000aa\",\"playingOnly\":true,\"maxSeconds\":120}");

        assertTrue(config.enabled());
        assertEquals(0.25, config.rate());
        assertEquals(120, config.maxSeconds());
        assertTrue(config.isTrusted(UUID.fromString("00000000-0000-0000-0000-0000000000aa")));
    }

    @Test
    void aPayloadThatDoesNotParseCapturesNothing() {
        assertEquals(AnticheatConfig.DISABLED, AnticheatCapture.parse("not json"));
    }

    /// Without posthog (the dev server), the flag reads disabled and so does the config.
    @Test
    void noPosthogMeansDisabled() {
        assertEquals(AnticheatConfig.DISABLED, AnticheatCapture.config());
    }

    private static List<CaptureControl> controlMessages(Collector<PluginMessagePacket> packets) {
        return packets.collect().stream()
            .filter(packet -> AnticheatCapture.CHANNEL.equals(packet.channel()))
            .map(packet -> CaptureControl.decode(packet.data()))
            .toList();
    }
}
