package net.hollowcube.apiserver.common;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

/// SHA-256, and the two ways the api writes one down: base64 on the wire and in a fingerprint, hex
/// in an object key. Both spellings are Go compatibility surfaces.
public final class Digest {

    public static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is unavailable", e);
        }
    }

    public static byte[] sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    /// A digest still being fed, for a body too large to hold.
    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is unavailable", e);
        }
    }

    /// `stream`, with every byte read from it fed into each of `digests` — a body large enough to
    /// stream is too large to read twice, and a replay commit wants two digests over one pass.
    public static InputStream tee(InputStream stream, MessageDigest... digests) {
        var out = stream;
        for (var digest : digests) out = new DigestInputStream(out, digest);
        return out;
    }

    public static String base64(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    public static String hex(byte[] value) {
        return HexFormat.of().formatHex(value);
    }

    private Digest() {
    }
}
