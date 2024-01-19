package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoopPlayerInviteService implements PlayerInviteService {
    @Override
    public void join(@NotNull Player sender, @NotNull Player target) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void accept(@NotNull Player sender, @Nullable String targetId) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void reject(@NotNull Player sender, @Nullable String targetId) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void registerInvite(@NotNull Player sender, @NotNull String targetId) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void registerRequest(@NotNull Player sender, @NotNull String targetId) {
        sender.sendMessage("not implemented");
    }
}
