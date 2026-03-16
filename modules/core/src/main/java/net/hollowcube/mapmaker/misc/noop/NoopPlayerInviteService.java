package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class NoopPlayerInviteService implements PlayerInviteService {
    @Override
    public void join(Player sender, String targetId) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void accept(Player sender, @Nullable String targetId) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void reject(Player sender, @Nullable String targetId) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void registerInvite(Player sender, String targetId) {
        sender.sendMessage("not implemented");
    }

    @Override
    public void registerRequest(Player sender, String targetId) {
        sender.sendMessage("not implemented");
    }
}
