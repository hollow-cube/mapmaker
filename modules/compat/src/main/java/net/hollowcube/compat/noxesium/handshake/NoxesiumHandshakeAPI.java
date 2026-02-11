package net.hollowcube.compat.noxesium.handshake;

import net.hollowcube.compat.noxesium.packets.v3.ClientboundHandshakeAcknowledgePacket;
import net.hollowcube.compat.noxesium.packets.v3.ClientboundHandshakeCancelPacket;
import net.hollowcube.compat.noxesium.packets.v3.ServerboundHandshakePacket;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class NoxesiumHandshakeAPI {

    private static final String ENCRYPTION_KEY = "gd2sWBVZNlpuN/iL26fS5CbEOsqVQJlY0lu8lL/8K8A=";
    private static final String ID = "noxesium-common";
    private static final String ENCRYPTION_ID = "aYT0P1Qae2k/OFyfF5cy+g==";
    private static final byte[] IV_PARAMETERS = new byte[] {-76, 14, 22, -123, 63, 60, -50, 23, -118, 10, 105, -127, 85, 41, -97, 37};

    public static void handleHandshake(Player player, ServerboundHandshakePacket packet) {
        var secret = packet.entrypoints().get(ENCRYPTION_ID);
        if (secret != null) {
            var decrypted = decryptSecret(secret);
            if (decrypted != null) {
                new ClientboundHandshakeAcknowledgePacket(Map.of(ID, decrypted)).send(player);
                return;
            }
        }
        new ClientboundHandshakeCancelPacket(ClientboundHandshakeCancelPacket.NO_MATCHING_ENTRYPOINTS).send(player);
    }

    private static String decryptSecret(String encryptedSecret) {
        try {
            var bytes = Base64.getDecoder().decode(ENCRYPTION_KEY);
            var key = new SecretKeySpec(bytes, "AES");
            var cupher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cupher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV_PARAMETERS));
            return new String(cupher.doFinal(Base64.getDecoder().decode(encryptedSecret)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
        return null;
    }
}
