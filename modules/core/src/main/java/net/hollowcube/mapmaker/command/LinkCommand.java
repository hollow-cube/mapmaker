package net.hollowcube.mapmaker.command;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LinkCommand extends CommandDsl {
    private final Argument<String> secretArg = Argument.Word("secret");

    private final PlayerService playerService;

    @Inject
    public LinkCommand(@NotNull PlayerService playerService) {
        super("link");

        this.playerService = playerService;

        addSyntax(playerOnly(this::showInformation));
        addSyntax(playerOnly(this::handleLinkWithSecret), secretArg);
    }

    private void showInformation(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage(LanguageProviderV2.translateMultiMerged("command.link.help", List.of()));
    }

    private void handleLinkWithSecret(@NotNull Player player, @NotNull CommandContext context) {
        var playerId = player.getUuid().toString();
        var secret = context.get(secretArg);

        var result = playerService.attemptVerify(playerId, secret);
        player.sendMessage(Component.translatable(switch (result) {
            case SUCCESS -> "command.link.success";
            case ALREADY_LINKED -> "command.link.already_linked";
            case INVALID_SECRET -> "command.link.invalid_secret";
            case EXPIRED_SECRET -> "command.link.expired_secret";
            case INTERNAL_ERROR -> "command.link.internal_error";
        }));
    }

}
