package net.hollowcube.mapmaker.command;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.dsl.SimpleCommand;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.mapmaker.gui.totp.QrCodeView;
import net.hollowcube.mapmaker.gui.totp.TotpInputView;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import net.minestom.server.entity.Player;

public class TotpCommand extends CommandDsl {

    private final PlayerService service;

    public TotpCommand(PlayerService service) {
        super("2fa");

        this.service = service;

        this.category = CommandCategory.DEFAULT;
        this.description = "Two-factor authentication commands";

        addSubcommand(SimpleCommand.of("enable")
                .description("Set up two-factor authentication")
                .callback(this::onEnable).build()
        );

        addSubcommand(SimpleCommand.of("disable")
                .description("Disable two-factor authentication")
                .callback(this::onDisable).build()
        );
    }

    private void onEnable(Player player) {
        String playerId = player.getUuid().toString();
        TotpSetupResponse response = this.service.beginTotpSetup(playerId);
        if (response == null) {
            player.sendMessage("You already have two-factor authentication enabled.");
        } else {
            var afterScan = new TotpInputView(
                "Enter 2FA Code",
                this.service::completeTotpSetup,
                result -> {
                    switch (result) {
                        case ALREADY_ENABLED -> {
                            player.sendMessage("Two-factor authentication is already enabled.");
                            player.closeInventory();
                        }
                        case SUCCESS -> player.openBook(TotpInputView.backupCodesBook(response.recoveryCodes()));
                    }
                }
            );
            Panel.open(player, new QrCodeView(response.qrCode(), response.qrCodeSize(), afterScan));
        }
    }

    private void onDisable(Player player) {
        String playerId = player.getUuid().toString();
        if (this.service.checkTotp(playerId, null) == PlayerService.TotpResult.NOT_ENABLED) {
            player.sendMessage("You do not have two-factor authentication enabled.");
        } else {
            Panel.open(player, new TotpInputView(
                "Enter 2FA Code",
                this.service::checkTotp,
                result -> {
                    if (result == PlayerService.TotpResult.NOT_ENABLED || service.removeTotp(playerId) == PlayerService.TotpResult.SUCCESS) {
                        player.sendMessage("Two-factor authentication has been disabled.");
                    } else {
                        player.sendMessage("Error disabling two-factor authentication.");
                    }
                    player.closeInventory();
                }
            ));
        }
    }
}
