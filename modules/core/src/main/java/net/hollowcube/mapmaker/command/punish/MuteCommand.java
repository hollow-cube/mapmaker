package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;

public class MuteCommand extends AbstractPunishCommand {

    public MuteCommand(PunishmentService service, PlayerService playerService) {
        super("mute", PunishmentType.MUTE, service, playerService);
    }
}
