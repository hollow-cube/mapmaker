package net.hollowcube.proxy.anticheat;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import net.hollowcube.anticheat.capture.CaptureClock;
import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.Protocol776;
import net.hollowcube.anticheat.protocol.ProtocolState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/// Who gets tapped, where the tap lands in the pipeline, and that a connection is left exactly as
/// it was found when it does not.
class AnticheatConnectionsTest {

    private static final Logger logger = LoggerFactory.getLogger(AnticheatConnectionsTest.class);
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @TempDir
    Path directory;

    /// The metrics are process-wide statics, so each test starts them from zero.
    @BeforeEach
    void resetMetrics() {
        AnticheatMetrics.connections.clear();
        AnticheatMetrics.ringBytes.clear();
        AnticheatMetrics.dropped.clear();
    }

    @Test
    void testTapGoesInAheadOfVia() {
        var installer = installer(true);
        var channel = TapPipeline.channel(null, true);

        assertNotNull(installer.join(player(channel), PLAYER, "Tester", 776, () -> null));

        var names = channel.pipeline().names();
        assertEquals(names.indexOf(AnticheatTap.VIA_DECODER) - 1, names.indexOf(AnticheatTap.DECODER_NAME));
        assertEquals(names.indexOf(AnticheatTap.VIA_ENCODER) - 1, names.indexOf(AnticheatTap.ENCODER_NAME));
        assertEquals(1, AnticheatMetrics.connections.labels("776", "true").get());

        installer.quit(PLAYER);
        assertEquals(0, AnticheatMetrics.connections.labels("776", "true").get());
        assertFalse(channel.finishAndReleaseAll());
    }

    @Test
    void testTapGoesInAheadOfTheCodecWithoutVia() {
        var installer = installer(true);
        var channel = TapPipeline.channel(null, false);

        assertNotNull(installer.join(player(channel), PLAYER, "Tester", 776, () -> null));

        var names = channel.pipeline().names();
        assertEquals(names.indexOf(AnticheatTap.MINECRAFT_DECODER) - 1, names.indexOf(AnticheatTap.DECODER_NAME));
        assertEquals(names.indexOf(AnticheatTap.MINECRAFT_ENCODER) - 1, names.indexOf(AnticheatTap.ENCODER_NAME));

        installer.close();
        assertFalse(channel.finishAndReleaseAll());
    }

    @Test
    void testUnsupportedProtocolVersionIsNotTapped() {
        var installer = installer(true);
        var channel = TapPipeline.channel(null, true);

        assertNull(installer.join(player(channel), PLAYER, "Tester", 775, () -> null));

        assertFalse(names(channel).contains(AnticheatTap.DECODER_NAME));
        assertFalse(names(channel).contains(AnticheatTap.ENCODER_NAME));
        assertEquals(1, AnticheatMetrics.dropped.labels("unsupported_pvn").get());
        assertEquals(1, AnticheatMetrics.connections.labels("775", "false").get());

        installer.quit(PLAYER);
        assertEquals(0, AnticheatMetrics.connections.labels("775", "false").get());
        assertFalse(channel.finishAndReleaseAll());
    }

    @Test
    void testDisabledInstallsNothing() {
        var installer = installer(false);
        var channel = TapPipeline.channel(null, true);

        assertNull(installer.join(player(channel), PLAYER, "Tester", 776, () -> null));

        assertFalse(names(channel).contains(AnticheatTap.DECODER_NAME));
        assertEquals(0, AnticheatMetrics.dropped.labels("unsupported_pvn").get());
        assertFalse(channel.finishAndReleaseAll());
    }

    /// A velocity that no longer hands out a channel must cost the player nothing.
    @Test
    void testAPlayerWithNoChannelIsNotTapped() {
        var installer = installer(true);

        assertNull(installer.join(new Object(), PLAYER, "Tester", 776, () -> null));
        assertEquals(1, AnticheatMetrics.dropped.labels("unsupported_pvn").get());
    }

    /// Velocity's pipeline is what it is; an unrecognisable one is left alone rather than guessed at.
    @Test
    void testAnUnknownPipelineIsLeftAlone() {
        var installer = installer(true);
        var channel = new EmbeddedChannel();

        assertNull(installer.join(player(channel), PLAYER, "Tester", 776, () -> null));
        assertFalse(names(channel).contains(AnticheatTap.DECODER_NAME));
        assertFalse(channel.finishAndReleaseAll());
    }

    /// The gauges nobody but the connections can answer: what their rings are holding, and what the
    /// ring cap has cost them. Both are read on a timer rather than published per frame.
    @Test
    void testSampleReportsRingBytesAndWhatTheRingCapDropped() {
        var installer = installer(true, 4096);
        var channel = TapPipeline.channel(null, false);

        assertNotNull(installer.join(player(channel), PLAYER, "Tester", 776, () -> null));
        play(channel);
        for (int frame = 0; frame < 200; frame++)
            channel.writeInbound(TapPipeline.buffer(TapPipeline.frame(
                id(ProtocolState.PLAY, Direction.C2S, "move_player_pos"), new byte[25])));
        channel.releaseInbound();

        installer.sample();
        double ringBytes = AnticheatMetrics.ringBytes.get();
        assertTrue(ringBytes > 0 && ringBytes <= 4096, "the ring gauge reads " + ringBytes);
        assertTrue(AnticheatMetrics.dropped.labels("ring_cap").get() > 0, "the ring cap dropped nothing");

        // A second look with nothing new charges nothing twice.
        double dropped = AnticheatMetrics.dropped.labels("ring_cap").get();
        installer.sample();
        assertEquals(dropped, AnticheatMetrics.dropped.labels("ring_cap").get());

        installer.quit(PLAYER);
        assertFalse(channel.finishAndReleaseAll());
    }

    /// Both directions through the configuration boundary, which is where the tap goes in.
    private static void play(EmbeddedChannel channel) {
        channel.writeOutbound(TapPipeline.buffer(TapPipeline.frame(
            id(ProtocolState.CONFIGURATION, Direction.S2C, "finish_configuration"), new byte[0])));
        channel.writeInbound(TapPipeline.buffer(TapPipeline.frame(
            id(ProtocolState.CONFIGURATION, Direction.C2S, "finish_configuration"), new byte[0])));
        channel.releaseOutbound();
        channel.releaseInbound();
    }

    private static int id(ProtocolState state, Direction direction, String name) {
        return Protocol776.packetId(state, direction, name);
    }

    private AnticheatConnections installer(boolean enabled) {
        return installer(enabled, 1 << 20);
    }

    private AnticheatConnections installer(boolean enabled, long ringMaxBytes) {
        var config = new AnticheatConfig(enabled, directory, Duration.ofSeconds(60),
                ringMaxBytes, 1 << 20, Duration.ofSeconds(1));
        return new AnticheatConnections(config, CaptureClock.SYSTEM, "proxy-test", "test", () -> false,
            (path, header) -> {
            });
    }

    private static Object player(Channel channel) {
        return new VelocityInternalsTest.FakePlayer(new VelocityInternalsTest.FakeConnection(channel));
    }

    private static List<String> names(EmbeddedChannel channel) {
        return channel.pipeline().names();
    }
}
