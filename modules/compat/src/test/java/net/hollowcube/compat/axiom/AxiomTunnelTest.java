package net.hollowcube.compat.axiom;

import com.github.luben.zstd.Zstd;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static net.hollowcube.compat.axiom.Bytes.*;
import static org.junit.jupiter.api.Assertions.*;

class AxiomTunnelTest {

    private static final int FIRST = AxiomTunnel.FRAGMENT_FIRST;
    private static final int LAST = AxiomTunnel.FRAGMENT_LAST;
    private static final int SPLIT = AxiomAPI.TUNNEL_SPLIT_SIZE;

    private final AxiomTunnel tunnel = new AxiomTunnel();

    private static byte[] fragment(int flags, byte[] data) {
        return concat(of(flags), data);
    }

    private static byte[] raw(String channel, byte[] payload) {
        return concat(string(channel), int32(payload.length), of(0), payload);
    }

    private static byte[] compressed(String channel, byte[] payload) {
        return concat(string(channel), int32(payload.length), of(1), Zstd.compress(payload));
    }

    /// Splits the way the reference client does: SPLIT - 1 data bytes per fragment plus the flag byte.
    private static List<byte[]> split(byte[] message) {
        var fragments = new ArrayList<byte[]>();
        for (int offset = 0; offset < message.length; offset += SPLIT - 1) {
            int end = Math.min(message.length, offset + SPLIT - 1);
            int flags = (offset == 0 ? FIRST : 0) | (end == message.length ? LAST : 0);
            fragments.add(fragment(flags, Arrays.copyOfRange(message, offset, end)));
        }
        return fragments;
    }

    private byte[] assemble(List<byte[]> fragments) throws AxiomTunnel.MalformedException {
        byte[] assembled = null;
        for (int i = 0; i < fragments.size(); i++) {
            assembled = tunnel.accept(fragments.get(i));
            assertEquals(i == fragments.size() - 1, assembled != null, "fragment " + i);
        }
        return assembled;
    }

    @Test
    void rawCompleteMessage() throws Exception {
        var payload = repeat(40, 1);
        var assembled = tunnel.accept(fragment(FIRST | LAST, raw("axiom:set_block", payload)));
        var message = AxiomTunnel.decode(assembled);
        assertEquals("axiom:set_block", message.channel());
        assertArrayEquals(payload, message.payload());
        assertFalse(tunnel.hasPartial());
    }

    @Test
    void compressedSingleFragment() throws Exception {
        var payload = repeat(3000, 2);
        var assembled = tunnel.accept(fragment(FIRST | LAST, compressed("axiom:set_buffer", payload)));
        var message = AxiomTunnel.decode(assembled);
        assertArrayEquals(payload, message.payload());
    }

    @Test
    void compressedMultipleFragments() throws Exception {
        var payload = new byte[600_000];
        new Random(600_000).nextBytes(payload);
        var message = compressed("axiom:set_buffer", payload);
        var fragments = split(message);
        assertTrue(fragments.size() > 2, "expected several fragments, got " + fragments.size());

        var assembled = assemble(fragments);
        assertArrayEquals(message, assembled);
        assertArrayEquals(payload, AxiomTunnel.decode(assembled).payload());
        assertFalse(tunnel.hasPartial());
    }

    @Test
    void fragmentAtSplitSizeIsAcceptedAndOneOverIsNot() throws Exception {
        assertNull(tunnel.accept(fragment(FIRST, repeat(SPLIT - 1, 4))));
        assertTrue(tunnel.hasPartial());

        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(fragment(0, repeat(SPLIT, 5))));
        assertFalse(tunnel.hasPartial());
    }

    @Test
    void firstReplacesAnIncompleteMessage() throws Exception {
        assertNull(tunnel.accept(fragment(FIRST, repeat(100, 6))));
        var payload = repeat(10, 7);
        var assembled = tunnel.accept(fragment(FIRST | LAST, raw("axiom:set_block", payload)));
        assertArrayEquals(raw("axiom:set_block", payload), assembled);
    }

    @Test
    void continuationWithoutFirstIsRejected() {
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(fragment(0, repeat(10, 8))));
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(fragment(LAST, repeat(10, 8))));
    }

    @Test
    void lastCompletesAndReleasesTheAssembly() throws Exception {
        tunnel.accept(fragment(FIRST, repeat(10, 9)));
        assertNotNull(tunnel.accept(fragment(LAST, repeat(10, 9))));
        assertFalse(tunnel.hasPartial());
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(fragment(LAST, repeat(10, 9))));
    }

    @Test
    void emptyFragmentsAreRejected() {
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(new byte[0]));
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(of(FIRST | LAST)));
    }

    @Test
    void unknownFragmentFlagsAreRejected() {
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(fragment(4, repeat(10, 10))));
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(fragment(FIRST | 8, repeat(10, 10))));
    }

    @Test
    void oversizedMessageIsRejectedBeforeItIsBuffered() throws Exception {
        var data = repeat(SPLIT - 1, 11);
        int fragments = AxiomAPI.MAX_TUNNEL_PACKET_SIZE / data.length;
        assertNull(tunnel.accept(fragment(FIRST, data)));
        for (int i = 1; i < fragments; i++) assertNull(tunnel.accept(fragment(0, data)));

        int remaining = AxiomAPI.MAX_TUNNEL_PACKET_SIZE - fragments * data.length;
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(fragment(0, repeat(remaining + 1, 12))));
        assertFalse(tunnel.hasPartial());
    }

    @Test
    void exactlyMaximumSizeIsAccepted() throws Exception {
        var data = repeat(SPLIT - 1, 13);
        int fragments = AxiomAPI.MAX_TUNNEL_PACKET_SIZE / data.length;
        assertNull(tunnel.accept(fragment(FIRST, data)));
        for (int i = 1; i < fragments; i++) assertNull(tunnel.accept(fragment(0, data)));
        int remaining = AxiomAPI.MAX_TUNNEL_PACKET_SIZE - fragments * data.length;
        var assembled = tunnel.accept(fragment(LAST, repeat(remaining, 14)));
        assertEquals(AxiomAPI.MAX_TUNNEL_PACKET_SIZE, assembled.length);
    }

    @Test
    void truncatedHeaderIsRejected() {
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(string("axiom:set_block")));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(concat(string("axiom:set_block"), of(0, 0, 0, 1))));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(of(0x20, 'a')));
    }

    @Test
    void invalidChannelIsRejected() {
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(raw("Not Valid!", repeat(4, 15))));
    }

    @Test
    void declaredSizeOutOfBoundsIsRejected() {
        var tooLarge = concat(string("axiom:set_block"), int32(AxiomAPI.MAX_TUNNEL_PACKET_SIZE + 1), of(1), repeat(8, 16));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(tooLarge));
        var negative = concat(string("axiom:set_block"), int32(-1), of(0), repeat(8, 16));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(negative));
    }

    @Test
    void unknownPacketFlagsAreRejected() {
        var message = concat(string("axiom:set_block"), int32(4), of(2), repeat(4, 17));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(message));
    }

    @Test
    void rawLengthMustMatchDeclaredSize() {
        var message = concat(string("axiom:set_block"), int32(5), of(0), repeat(4, 18));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(message));
    }

    @Test
    void malformedZstdIsRejected() {
        var message = concat(string("axiom:set_block"), int32(64), of(1), repeat(20, 19));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(message));
    }

    @Test
    void decompressedSizeMustMatchDeclaredSize() {
        var payload = repeat(500, 20);
        var larger = concat(string("axiom:set_block"), int32(payload.length + 1), of(1), Zstd.compress(payload));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(larger));
        var smaller = concat(string("axiom:set_block"), int32(payload.length - 1), of(1), Zstd.compress(payload));
        assertThrows(AxiomTunnel.MalformedException.class, () -> AxiomTunnel.decode(smaller));
    }

    @Test
    void resetDropsPartialData() throws Exception {
        tunnel.accept(fragment(FIRST, repeat(10, 21)));
        tunnel.reset();
        assertFalse(tunnel.hasPartial());
        assertThrows(AxiomTunnel.MalformedException.class, () -> tunnel.accept(fragment(LAST, repeat(10, 21))));
    }
}
