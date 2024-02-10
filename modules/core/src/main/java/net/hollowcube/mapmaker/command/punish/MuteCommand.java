package net.hollowcube.mapmaker.command.punish;

import com.google.inject.Inject;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.PunishmentType;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;

public class MuteCommand extends AbstractPunishCommand {

    @Inject
    public MuteCommand(@NotNull PunishmentService service, @NotNull SessionManager sessionManager) {
        super("mute", PunishmentType.MUTE, service, sessionManager);
    }
}
