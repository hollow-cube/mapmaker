package net.hollowcube.proxy.anticheat;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VelocityInternalsTest {
    /// Shaped like ConnectedPlayer -> MinecraftConnection -> Channel.
    public record FakePlayer(FakeConnection getConnection) {
    }

    public record FakeConnection(Channel getChannel) {
    }

    /// A velocity that renamed or dropped MinecraftConnection#getChannel.
    public record ChannellessPlayer(Channelless getConnection) {
    }

    public record Channelless() {
    }

    /// A velocity where getChannel no longer returns a netty channel.
    public record OtherChannelPlayer(OtherChannel getConnection) {
    }

    public record OtherChannel(String getChannel) {
    }

    /// Shaped like VelocityServer#isShuttingDown.
    public record FakeProxy(boolean isShuttingDown) {
    }

    @Test
    void testResolvesChannel() {
        var channel = new EmbeddedChannel();
        var player = new FakePlayer(new FakeConnection(channel));

        assertSame(channel, VelocityInternals.channelOf(player));
        assertSame(channel, VelocityInternals.channelOf(player)); // cached handles
    }

    @Test
    void testShuttingDown() {
        assertTrue(VelocityInternals.isShuttingDown(new FakeProxy(true)));
        assertFalse(VelocityInternals.isShuttingDown(new FakeProxy(false)));
    }

    /// A velocity without the method is not a proxy that is shutting down: every disconnect then
    /// reads the way it did before, rather than every one of them reading as a shutdown.
    @Test
    void testMissingIsShuttingDown() {
        assertFalse(VelocityInternals.isShuttingDown(new Object()));
    }

    @Test
    void testMissingGetConnection() {
        assertNull(VelocityInternals.channelOf(new Object()));
    }

    @Test
    void testNullConnection() {
        assertNull(VelocityInternals.channelOf(new FakePlayer(null)));
    }

    @Test
    void testMissingGetChannel() {
        assertNull(VelocityInternals.channelOf(new ChannellessPlayer(new Channelless())));
    }

    @Test
    void testWrongChannelType() {
        assertNull(VelocityInternals.channelOf(new OtherChannelPlayer(new OtherChannel("nope"))));
    }
}
