package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import org.jetbrains.annotations.NotNull;

public class MuteCommand extends AbstractPunishCommand {

    public MuteCommand(@NotNull PunishmentService service, @NotNull PlayerClient players) {
        super("mute", PunishmentType.MUTE, service, players);
    }
}
