package net.hollowcube.mapmaker.invite;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PlayerInviteService {

    void join(@NotNull Player sender, @NotNull Player target);

    void accept(@NotNull Player sender);

    void accept(@NotNull Player sender, @NotNull Player target);

    void reject(@NotNull Player sender, @NotNull Player target);

    void registerInvite(@NotNull Player sender, @NotNull Player target);

    void registerRequest(@NotNull Player sender, @NotNull Player target);

}
