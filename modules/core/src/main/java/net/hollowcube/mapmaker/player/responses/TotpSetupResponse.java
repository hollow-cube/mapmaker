package net.hollowcube.mapmaker.player.responses;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record TotpSetupResponse(
        String uri,
        int qrCodeSize,
        String qrCode,
        String[] recoveryCodes
) {
}
