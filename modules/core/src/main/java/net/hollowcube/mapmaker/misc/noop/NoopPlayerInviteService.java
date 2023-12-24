package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NoopPlayerInviteService implements PlayerInviteService {
    @Override
    public void join(@NotNull Player sender, @NotNull Player target) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void accept(@NotNull Player sender) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void accept(@NotNull Player sender, @NotNull Player target) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void reject(@NotNull Player sender, @NotNull Player target) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void registerInvite(@NotNull Player sender, @NotNull Player target) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void registerRequest(@NotNull Player sender, @NotNull Player target) {
        sender.sendMessage("not implemented");
    }
}
