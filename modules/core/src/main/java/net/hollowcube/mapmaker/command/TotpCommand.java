package net.hollowcube.mapmaker.command;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.dsl.SimpleCommand;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.mapmaker.gui.totp.QrCodeView;
import net.hollowcube.mapmaker.gui.totp.TotpInputViews;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class TotpCommand extends CommandDsl {

    public TotpCommand(@NotNull PlayerService service, @NotNull Controller guis) {
        super("2fa");

        this.category = CommandCategory.DEFAULT;
        this.description = "Two-factor authentication commands";

        addSubcommand(SimpleCommand.of("enable")
                .description("Set up two-factor authentication")
                .callback(player -> {
                    String playerId = player.getUuid().toString();
                    TotpSetupResponse response = service.beginTotpSetup(playerId);
                    if (response == null) {
                        player.sendMessage("You already have two-factor authentication enabled.");
                    } else {
                        var input = TotpInputViews.inputView(
                                Component.text("Enter 2FA Code"),
                                service::completeTotpSetup,
                                (view, result) -> {
                                    switch (result) {
                                        case ALREADY_ENABLED -> {
                                            player.sendMessage("Two-factor authentication is already enabled.");
                                            player.closeInventory();
                                        }
                                        case SUCCESS -> player.openBook(TotpInputViews.backupCodesBook(response.recoveryCodes()));
                                    }
                                });
                        guis.show(player, context -> new QrCodeView(context, response.qrCode(), response.qrCodeSize(), input));
                    }
                }).build()
        );

        addSubcommand(SimpleCommand.of("disable")
                .description("Disable two-factor authentication")
                .callback(player -> {
                    String playerId = player.getUuid().toString();
                    if (service.checkTotp(playerId, null) == PlayerService.TotpResult.NOT_ENABLED) {
                        player.sendMessage("You do not have two-factor authentication enabled.");
                    } else {
                        var input = TotpInputViews.inputView(
                                Component.text("Enter 2FA Code"),
                                service::checkTotp,
                                (view, result) -> {
                                    if (result == PlayerService.TotpResult.NOT_ENABLED || service.removeTotp(playerId) == PlayerService.TotpResult.SUCCESS) {
                                        player.sendMessage("Two-factor authentication has been disabled.");
                                    } else {
                                        player.sendMessage("Error disabling two-factor authentication.");
                                    }
                                    player.closeInventory();
                                });
                        guis.show(player, input);
                    }
                }).build()
        );
    }
}
