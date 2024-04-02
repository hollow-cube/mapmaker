package net.hollowcube.mapmaker.command.punish;

import com.google.inject.Inject;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import org.jetbrains.annotations.NotNull;

public class BanCommand extends AbstractPunishCommand {

    @Inject
    public BanCommand(@NotNull PunishmentService service, @NotNull PlayerService playerService, @NotNull PermManager permManager) {
        super("ban", PunishmentType.BAN, service, playerService, permManager);
    }
}
