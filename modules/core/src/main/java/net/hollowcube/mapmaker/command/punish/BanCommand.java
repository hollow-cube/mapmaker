package net.hollowcube.mapmaker.command.punish;

import com.google.inject.Inject;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.PunishmentType;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;

public class BanCommand extends AbstractPunishCommand {

    @Inject
    public BanCommand(@NotNull PunishmentService service, @NotNull SessionManager sessionManager) {
        super("ban", PunishmentType.BAN, service, sessionManager);
    }
}
