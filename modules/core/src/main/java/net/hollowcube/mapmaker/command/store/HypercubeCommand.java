package net.hollowcube.mapmaker.command.store;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.mapmaker.gui.store.StoreModule;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Supplier;

public class HypercubeCommand extends CommandDsl {
    private final Supplier<ScriptEngine> scriptEngine;
    private final PlayerService playerService;
    private final PermManager permManager;

    public HypercubeCommand(@NotNull Supplier<ScriptEngine> scriptEngine, @NotNull PlayerService playerService, @NotNull PermManager permManager) {
        super("hypercube");
        this.scriptEngine = scriptEngine;
        this.playerService = playerService;
        this.permManager = permManager;

        addSyntax(playerOnly(this::handleHypercubeInfo));
    }

    private void handleHypercubeInfo(@NotNull Player player, @NotNull CommandContext context) {
        try {
            var playerId = PlayerDataV2.fromPlayer(player).id();
            var status = playerService.getHypercubeStatus(playerId);
            if (status == null) {
                StoreModule.openStoreView(scriptEngine.get(), playerService, permManager, player, "hypercube");
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
