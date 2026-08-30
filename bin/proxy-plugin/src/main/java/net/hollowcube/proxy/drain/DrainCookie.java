package net.hollowcube.proxy.drain;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/// The cookie a draining proxy hands the client on its way out, holding the transfer it was in the
/// middle of. It has to live on the client to survive the reconnect, and a client can rewrite
/// anything it holds, so it is sealed: the payload names a backend address the receiving proxy will
/// connect the player to, and a forgeable one would let anybody reach any address in the cluster.
///
/// AES-256-GCM, `[version:1][expiry:8][nonce:12][ciphertext + tag]`, with `version || playerId ||
/// expiry` as the additional data so a cookie replayed under another player or with the expiry
/// moved fails the tag rather than needing a check of its own.
public final class DrainCookie {
    /// Vanilla's cap on a cookie payload; a sealed cookie over this cannot be stored at all.
    public static final int MAX_COOKIE_BYTES = 5120;

    private static final byte VERSION = 1;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int HEADER_BYTES = 1 + Long.BYTES + NONCE_BYTES;

    public record Transfer(String address, byte[] transferData) {
    }

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public DrainCookie(SecretKey key) {
        this.key = key;
    }

    /// The key is the sha-256 of whatever `path` holds, so any secret of any shape works and every
    /// proxy given the same one agrees. Null when there is no path or nothing readable at it, which
    /// turns optimistic transfer off - a proxy that cannot seal must never fall back to plaintext.
    public static @Nullable DrainCookie load(Logger logger, @Nullable String path) {
        if (path == null || path.isBlank()) {
            logger.info("drain: no PROXY_COOKIE_SECRET_FILE, optimistic transfer disabled");
            return null;
        }
        try {
            var secret = Files.readString(Path.of(path), StandardCharsets.UTF_8).trim();
            if (secret.isEmpty()) throw new IOException("empty");
            var raw = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            logger.info("drain: cookie key loaded from {}", path);
            return new DrainCookie(new SecretKeySpec(raw, "AES"));
        } catch (IOException | GeneralSecurityException e) {
            logger.error("drain: unusable cookie key at {}, optimistic transfer disabled: {}", path, e.toString());
            return null;
        }
    }

    /// Check the result against [#MAX_COOKIE_BYTES] before storing it.
    public byte[] seal(UUID playerId, Instant expiry, String address, byte[] transferData) {
        var addressBytes = address.getBytes(StandardCharsets.UTF_8);
        var plain = ByteBuffer.allocate(Integer.BYTES + addressBytes.length + Integer.BYTES + transferData.length)
            .putInt(addressBytes.length).put(addressBytes)
            .putInt(transferData.length).put(transferData)
            .array();

        var nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        long expiryMillis = expiry.toEpochMilli();
        try {
            var cipher = cipher(Cipher.ENCRYPT_MODE, playerId, nonce, expiryMillis);
            var sealed = cipher.doFinal(plain);
            return ByteBuffer.allocate(HEADER_BYTES + sealed.length)
                .put(VERSION).putLong(expiryMillis).put(nonce).put(sealed)
                .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to seal a drain cookie", e);
        }
    }

    /// Null for anything that does not open as this player's live cookie. Never throws: it is all
    /// client input, and a rejected cookie only means the player is routed as a fresh login.
    public @Nullable Transfer open(UUID playerId, Instant now, byte @Nullable [] cookie) {
        if (cookie == null || cookie.length <= HEADER_BYTES || cookie[0] != VERSION) return null;

        var buffer = ByteBuffer.wrap(cookie);
        buffer.get();
        long expiryMillis = buffer.getLong();
        var nonce = new byte[NONCE_BYTES];
        buffer.get(nonce);
        var sealed = new byte[buffer.remaining()];
        buffer.get(sealed);

        try {
            var plain = ByteBuffer.wrap(cipher(Cipher.DECRYPT_MODE, playerId, nonce, expiryMillis).doFinal(sealed));
            if (!now.isBefore(Instant.ofEpochMilli(expiryMillis))) return null;
            var address = new byte[plain.getInt()];
            plain.get(address);
            var transferData = new byte[plain.getInt()];
            plain.get(transferData);
            return new Transfer(new String(address, StandardCharsets.UTF_8), transferData);
        } catch (GeneralSecurityException | RuntimeException e) {
            return null;
        }
    }

    private Cipher cipher(int mode, UUID playerId, byte[] nonce, long expiryMillis) throws GeneralSecurityException {
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(ByteBuffer.allocate(1 + 3 * Long.BYTES)
            .put(VERSION)
            .putLong(playerId.getMostSignificantBits())
            .putLong(playerId.getLeastSignificantBits())
            .putLong(expiryMillis)
            .array());
        return cipher;
    }
}
