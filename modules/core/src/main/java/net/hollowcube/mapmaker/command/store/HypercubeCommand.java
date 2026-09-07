package net.hollowcube.mapmaker.command.store;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.AccountService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

import static net.hollowcube.mapmaker.gui.store.StoreView.TAB_HYPERCUBE;
import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public class HypercubeCommand extends CommandDsl {
    private final ApiClient api;
    private final AccountService accountService;

    public HypercubeCommand(@NotNull ApiClient api, @NotNull AccountService accountService) {
        super("hypercube");
        this.api = api;
        this.accountService = accountService;

        addSyntax(playerOnly(this::handleHypercubeInfo));
    }

    private void handleHypercubeInfo(@NotNull Player player, @NotNull CommandContext context) {
        try {
            var playerId = localPlayer(player).id();
            var hypercube = api.players.hypercube(playerId);
            if (hypercube == null) {
                Panel.open(player, new StoreView(accountService, TAB_HYPERCUBE));
                return;
            }

            player.sendMessage(GenericMessages.COMMAND_HYPERCUBE_SUBSCRIPTION_INFO.with(
                formatInstant(hypercube.start()), formatInstant(hypercube.end())
            ));
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(GenericMessages.COMMAND_UNKNOWN_ERROR);
        }
    }

    public static String formatInstant(Instant instant) {
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());

        String month = zonedDateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        int day = zonedDateTime.getDayOfMonth();
        String daySuffix = getDayOfMonthSuffix(day);
        int year = zonedDateTime.getYear();

        return String.format("%s %d%s, %d", month, day, daySuffix, year);
    }

    private static String getDayOfMonthSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

}
