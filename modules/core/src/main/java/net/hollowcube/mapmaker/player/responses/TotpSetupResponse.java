package net.hollowcube.mapmaker.player.responses;

public record TotpSetupResponse(
        String uri,
        int qrCodeSize,
        String qrCode,
        String[] recoveryCodes
) {
}
