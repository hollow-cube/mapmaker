package net.hollowcube.mapmaker.command.store;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

import static net.hollowcube.mapmaker.gui.store.StoreView.TAB_HYPERCUBE;

public class HypercubeCommand extends CommandDsl {
    private final PlayerService playerService;

    public HypercubeCommand(@NotNull PlayerService playerService) {
        super("hypercube");
        this.playerService = playerService;

        addSyntax(playerOnly(this::handleHypercubeInfo));
    }

    private void handleHypercubeInfo(@NotNull Player player, @NotNull CommandContext context) {
        try {
            var playerId = PlayerData.fromPlayer(player).id();
            var status = playerService.getHypercubeStatus(playerId);
            if (status == null) {
                Panel.open(player, new StoreView(playerService, TAB_HYPERCUBE));
                return;
            }

            player.sendMessage(GenericMessages.COMMAND_HYPERCUBE_SUBSCRIPTION_INFO.with(
                formatInstant(status.since()), formatInstant(status.until())
            ));
        } catch (Exception e) {
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
        switch (day % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

}
