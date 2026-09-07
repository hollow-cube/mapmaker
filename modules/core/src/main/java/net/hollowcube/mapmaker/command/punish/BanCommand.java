package net.hollowcube.mapmaker.command.punish;

import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import org.jetbrains.annotations.NotNull;

public class BanCommand extends AbstractPunishCommand {

    public BanCommand(@NotNull PunishmentService service, @NotNull PlayerService players) {
        super("ban", PunishmentType.BAN, service, players);
    }
}
