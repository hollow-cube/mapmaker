package net.hollowcube.compat.axiom;

import com.github.luben.zstd.Zstd;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.axiom.events.AxiomEnabledEvent;
import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.minestom.testing.TestConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static net.hollowcube.compat.axiom.Bytes.*;
import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class AxiomPlayerTest {

    private static final byte[] ENABLE_V10_HEADER = of(0x01, 0x00, 0x01, 0x00, 0x00, 0x79, 0x18, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00);
    private static final byte[] ENABLE_V9 = of(0x01, 0x00, 0x10, 0x00, 0x00, 0x01, 0x00, 0x00);

    private Env env;
    private TestConnection connection;
    private Player player;
    private long handshakeId;
    private final List<Boolean> enabledEvents = new ArrayList<>();

    @BeforeEach
    void setup(Env env) {
        this.env = env;
        PacketRegistryImpl.unsafeReset();
        CompatProvider.load(env.process().eventHandler());
        env.process().eventHandler().addListener(AxiomEnabledEvent.class, event -> enabledEvents.add(event.isEnabled()));

        var instance = env.createFlatInstance();
        connection = env.createConnection();
        var hello = connection.trackIncoming(PluginMessagePacket.class);
        player = connection.connect(instance, new Pos(0, 40, 0));

        var packets = channel(hello, "axiom:hello");
        assertEquals(1, packets.size(), "one hello on first spawn");
        assertEquals(8, packets.getFirst().data().length);
        handshakeId = ByteBuffer.wrap(packets.getFirst().data()).getLong();
        assertNotEquals(0, handshakeId);
    }

    private static List<PluginMessagePacket> channel(Collector<PluginMessagePacket> collector, String channel) {
        return collector.collect().stream().filter(packet -> packet.channel().equals(channel)).toList();
    }

    private void receive(String channel, byte[] payload) {
        env.process().eventHandler().call(new PlayerPluginMessageEvent(player, channel, payload));
    }

    private void helloApi10(long id) {
        receive("axiom:hello", concat(varInt(10), varInt(4900), varInt(MinecraftServer.PROTOCOL_VERSION), int64(id)));
    }

    private void helloApi9() {
        receive("axiom:hello", concat(varInt(9), varInt(4900), varInt(MinecraftServer.PROTOCOL_VERSION)));
    }

    @Test
    void buildModeWaitsForTheHandshakeReply() {
        var axiom = AxiomPlayer.get(player);
        var packets = connection.trackIncoming(PluginMessagePacket.class);
        axiom.setEnabled(true);
        assertFalse(axiom.isEnabled());
        assertEquals(List.of(), packets.collect());

        packets = connection.trackIncoming(PluginMessagePacket.class);
        helloApi10(handshakeId);
        assertTrue(axiom.isEnabled());
        assertEquals(AxiomAPI.API_10, axiom.api());
        var sent = packets.collect();
        assertEquals(List.of("axiom:enable", "axiom:register_world_properties", "axiom:restrictions"),
                sent.stream().map(PluginMessagePacket::channel).limit(3).toList());
        assertArrayEquals(ENABLE_V10_HEADER, Arrays.copyOf(sent.getFirst().data(), ENABLE_V10_HEADER.length));
        assertEquals(List.of(true), enabledEvents);
    }

    @Test
    void wrongOrReplayedHandshakeIdsAreRefused() {
        var axiom = AxiomPlayer.get(player);
        axiom.setEnabled(true);

        var packets = connection.trackIncoming(PluginMessagePacket.class);
        helloApi10(handshakeId + 1);
        assertEquals(1, channel(packets, "axiom:goodbye").size());
        assertEquals(-1, axiom.api());
        assertFalse(axiom.isEnabled());

        helloApi10(handshakeId);
        assertTrue(axiom.isEnabled());
        packets = connection.trackIncoming(PluginMessagePacket.class);
        helloApi10(handshakeId);
        helloApi9();
        assertEquals(List.of(), packets.collect());
        assertEquals(AxiomAPI.API_10, axiom.api());
        assertEquals(List.of(true), enabledEvents);
    }

    @Test
    void mismatchedMinecraftVersionStaysDisabled() {
        var axiom = AxiomPlayer.get(player);
        axiom.setEnabled(true);
        ProtocolVersions.unsafeSetProtocolVersion(player, MinecraftServer.PROTOCOL_VERSION - 1);

        var packets = connection.trackIncoming(PluginMessagePacket.class);
        helloApi10(handshakeId);
        var goodbye = channel(packets, "axiom:goodbye");
        assertEquals(1, goodbye.size());
        assertArrayEquals(string(AxiomPlayer.PROTOCOL_MISMATCH), goodbye.getFirst().data());
        assertEquals(List.of(), channel(packets, "axiom:enable"));
        assertFalse(axiom.isEnabled());

        helloApi9();
        assertEquals(-1, axiom.api());
    }

    @Test
    void api9ClientHellosOnItsOwnAndIsServedThroughTheQueue() {
        var axiom = AxiomPlayer.get(player);
        var packets = connection.trackIncoming(PluginMessagePacket.class);
        helloApi9();
        axiom.setEnabled(true);
        assertTrue(axiom.isEnabled());
        assertEquals(AxiomAPI.API_9, axiom.api());
        assertEquals(List.of(), packets.collect(), "nothing leaves before the client declares its channels");

        packets = connection.trackIncoming(PluginMessagePacket.class);
        receive("minecraft:register", "axiom:enable\0axiom:register_world_properties\0axiom:restrictions".getBytes());
        var enable = channel(packets, "axiom:enable");
        assertEquals(1, enable.size());
        assertArrayEquals(ENABLE_V9, enable.getFirst().data());
        assertEquals(1, channel(packets, "axiom:restrictions").size());
    }

    @Test
    void leavingBuildModeDisablesAndKeepsTheNegotiation() {
        var axiom = AxiomPlayer.get(player);
        helloApi10(handshakeId);
        axiom.setEnabled(true);

        var packets = connection.trackIncoming(PluginMessagePacket.class);
        axiom.setEnabled(false);
        assertFalse(axiom.isEnabled());
        var sent = packets.collect();
        assertEquals(List.of("axiom:enable", "axiom:register_world_properties"), sent.stream().map(PluginMessagePacket::channel).toList());
        assertArrayEquals(of(0x00), sent.get(0).data());
        assertArrayEquals(of(0x00), sent.get(1).data());

        packets = connection.trackIncoming(PluginMessagePacket.class);
        axiom.setEnabled(true);
        assertEquals(1, channel(packets, "axiom:enable").size());
        assertEquals(List.of(), channel(packets, "axiom:hello"));
        assertEquals(List.of(true, false, true), enabledEvents);
    }

    @Test
    void editorEntryAfterAPriorNegotiationEnablesOnlyAfterTheFreshReply() {
        var axiom = AxiomPlayer.get(player);
        helloApi10(handshakeId); // A prior negotiation on this connection, eg the playing state before the editor.
        assertEquals(AxiomAPI.API_10, axiom.api());
        assertFalse(axiom.isEnabled());

        // The editor transfer re-runs first spawn (a fresh hello) and asks to enable. The client discards its
        // editor state when it sees the hello, so enabling must wait for the reply or the snapshot is wiped.
        var packets = connection.trackIncoming(PluginMessagePacket.class);
        axiom.beginHandshake();
        assertEquals(-1, axiom.api(), "a fresh hello drops the stale negotiation");
        axiom.setEnabled(true);
        assertFalse(axiom.isEnabled(), "must not enable off the stale api");
        var hello = channel(packets, "axiom:hello");
        assertEquals(1, hello.size());
        assertEquals(List.of(), channel(packets, "axiom:enable"), "no enable before the client reset");

        packets = connection.trackIncoming(PluginMessagePacket.class);
        helloApi10(ByteBuffer.wrap(hello.getFirst().data()).getLong());
        assertTrue(axiom.isEnabled());
        assertEquals(1, channel(packets, "axiom:enable").size(), "enable resent after the reset");
    }

    @Test
    void redoHandshakeRenegotiatesBeforeResendingState() {
        var axiom = AxiomPlayer.get(player);
        helloApi10(handshakeId);
        axiom.setEnabled(true);

        var packets = connection.trackIncoming(PluginMessagePacket.class);
        axiom.redoHandshake();
        assertFalse(axiom.isEnabled());
        assertEquals(-1, axiom.api());
        var hello = channel(packets, "axiom:hello");
        assertEquals(1, hello.size());
        assertEquals(1, channel(packets, "axiom:enable").size());

        packets = connection.trackIncoming(PluginMessagePacket.class);
        helloApi10(ByteBuffer.wrap(hello.getFirst().data()).getLong());
        assertTrue(axiom.isEnabled());
        assertEquals(1, channel(packets, "axiom:enable").size());
    }

    @Test
    void tunneledPacketsReachTheSharedHandlerOnce() {
        var axiom = AxiomPlayer.get(player);
        var request = concat(int64(5), varInt(1), int64(0x1122334455667788L), int64(0x99AABBCCDDEEFF00L));
        var message = concat(string("axiom:request_entity_data"), int32(request.length), of(1), Zstd.compress(request));
        int half = message.length / 2;
        var first = concat(of(AxiomTunnel.FRAGMENT_FIRST), Arrays.copyOfRange(message, 0, half));
        var last = concat(of(AxiomTunnel.FRAGMENT_LAST), Arrays.copyOfRange(message, half, message.length));

        var packets = connection.trackIncoming(PluginMessagePacket.class);
        receive("axiom:tunnel", first);
        receive("axiom:tunnel", last);
        assertEquals(List.of(), packets.collect(), "tunnel is ignored until API 10 is negotiated and enabled");

        helloApi10(handshakeId);
        axiom.setEnabled(true);
        packets = connection.trackIncoming(PluginMessagePacket.class);
        receive("axiom:tunnel", first);
        receive("axiom:tunnel", last);
        var responses = channel(packets, "axiom:response_entity_data");
        assertEquals(1, responses.size());
        assertArrayEquals(int64(5), Arrays.copyOf(responses.getFirst().data(), 8));
        assertFalse(axiom.tunnel().hasPartial());
    }

    @Test
    void disconnectReleasesTheSession() {
        var axiom = AxiomPlayer.get(player);
        helloApi10(handshakeId);
        axiom.setEnabled(true);
        player.remove();
        assertNotSame(axiom, AxiomPlayer.get(player));
    }
}
