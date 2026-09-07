package net.hollowcube.mapmaker.command;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.dsl.SimpleCommand;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.mapmaker.gui.totp.QrCodeView;
import net.hollowcube.mapmaker.gui.totp.TotpInputView;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.AccountService;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.server.network.packet.server.play.CloseWindowPacket;
import org.jetbrains.annotations.NotNull;

public class TotpCommand extends CommandDsl {

    private final AccountService service;

    public TotpCommand(@NotNull AccountService service) {
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

    private void onEnable(@NotNull Player player) {
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
                        case SUCCESS -> {
                            player.sendPacket(new CloseWindowPacket(-1));
                            player.sendPacket(new ShowDialogPacket(TotpInputView.backupCodesDialog(response.recoveryCodes())));
                        }
                    }
                }
            );
            Panel.open(player, new QrCodeView(response.qrCode(), response.qrCodeSize(), afterScan));
        }
    }

    private void onDisable(@NotNull Player player) {
        String playerId = player.getUuid().toString();
        if (this.service.checkTotp(playerId, null) == AccountService.TotpResult.NOT_ENABLED) {
            player.sendMessage("You do not have two-factor authentication enabled.");
        } else {
            Panel.open(player, new TotpInputView(
                "Enter 2FA Code",
                this.service::checkTotp,
                result -> {
                    if (result == AccountService.TotpResult.NOT_ENABLED || service.removeTotp(playerId) == AccountService.TotpResult.SUCCESS) {
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
