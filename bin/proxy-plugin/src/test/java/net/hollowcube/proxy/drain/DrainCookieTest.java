package net.hollowcube.proxy.drain;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrainCookieTest {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DrainCookieTest.class);

    private static final UUID PLAYER = UUID.fromString("ee1e0b28-2f26-4ac1-9c4f-5f37d5c78e75");
    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");
    private static final Instant EXPIRY = NOW.plusSeconds(30);

    private final DrainCookie cookie = new DrainCookie(key(1));

    @Test
    void testRoundTrip() {
        var data = "{\"HubTransferData\":{}}".getBytes(StandardCharsets.UTF_8);
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", data);

        var transfer = cookie.open(PLAYER, NOW, sealed);
        assertNotNull(transfer);
        assertEquals("10.42.0.7", transfer.address());
        assertArrayEquals(data, transfer.transferData());
    }

    @Test
    void testRoundTripWithoutTransferData() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);

        var transfer = cookie.open(PLAYER, NOW, sealed);
        assertNotNull(transfer);
        assertEquals(0, transfer.transferData().length);
    }

    @Test
    void testNonceIsFreshPerSeal() {
        var first = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        var second = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        assertFalse(Arrays.equals(first, second));
    }

    @Test
    void testAbsent() {
        assertNull(cookie.open(PLAYER, NOW, null));
        assertNull(cookie.open(PLAYER, NOW, new byte[0]));
    }

    @Test
    void testTruncated() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        assertNull(cookie.open(PLAYER, NOW, Arrays.copyOf(sealed, 8)));
        assertNull(cookie.open(PLAYER, NOW, Arrays.copyOf(sealed, 30)));
    }

    @Test
    void testUnknownVersion() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        sealed[0] = 99;
        assertNull(cookie.open(PLAYER, NOW, sealed));
    }

    @Test
    void testTamperedCiphertext() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        sealed[sealed.length - 1] ^= 0x01;
        assertNull(cookie.open(PLAYER, NOW, sealed));
    }

    /// The point of the whole class: a player cannot pick up somebody else's cookie and be routed
    /// by it, because the uuid is bound into the tag.
    @Test
    void testAnotherPlayer() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        assertNull(cookie.open(UUID.randomUUID(), NOW, sealed));
    }

    /// Moving the expiry forward is the obvious way to make a cookie last; it is in the tag.
    @Test
    void testMovedExpiry() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        sealed[1] ^= 0x40;
        assertNull(cookie.open(PLAYER, NOW, sealed));
    }

    @Test
    void testExpired() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        assertNull(cookie.open(PLAYER, EXPIRY, sealed));
        assertNull(cookie.open(PLAYER, EXPIRY.plusSeconds(1), sealed));
    }

    @Test
    void testAnotherProxysKey() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0]);
        assertNull(new DrainCookie(key(2)).open(PLAYER, NOW, sealed));
    }

    @Test
    void testOversizeIsVisibleToTheCaller() {
        var sealed = cookie.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[DrainCookie.MAX_COOKIE_BYTES]);
        assertTrue(sealed.length > DrainCookie.MAX_COOKIE_BYTES);
    }

    /// Any secret of any shape, hashed to a key, so `openssl rand -base64 32` and `local dev` both
    /// work and two proxies given the same file agree.
    @Test
    void testLoadHashesWhateverIsInTheFile() throws Exception {
        var file = Files.createTempFile("drain-cookie", ".secret");
        Files.writeString(file, "local dev\n");
        try {
            var one = DrainCookie.load(logger, file.toString());
            var two = DrainCookie.load(logger, file.toString());
            assertNotNull(one);
            assertNotNull(two);
            assertNotNull(two.open(PLAYER, NOW, one.seal(PLAYER, EXPIRY, "10.42.0.7", new byte[0])));
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void testLoadRefusesWhatItCannotUse() throws Exception {
        assertNull(DrainCookie.load(logger, null));
        assertNull(DrainCookie.load(logger, "  "));
        assertNull(DrainCookie.load(logger, "/nonexistent/drain-cookie.secret"));

        var empty = Files.createTempFile("drain-cookie", ".secret");
        Files.writeString(empty, "   \n");
        try {
            assertNull(DrainCookie.load(logger, empty.toString()));
        } finally {
            Files.delete(empty);
        }
    }

    private static SecretKeySpec key(int seed) {
        var raw = new byte[32];
        Arrays.fill(raw, (byte) seed);
        return new SecretKeySpec(raw, "AES");
    }
}
