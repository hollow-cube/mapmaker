package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;

public class BanCommand extends AbstractPunishCommand {

    public BanCommand(PunishmentService service, PlayerService playerService) {
        super("ban", PunishmentType.BAN, service, playerService);
    }
}
