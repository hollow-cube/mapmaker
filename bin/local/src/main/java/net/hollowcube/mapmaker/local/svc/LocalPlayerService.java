package net.hollowcube.mapmaker.local.svc;

import net.hollowcube.mapmaker.misc.noop.NoopPlayerService;
import net.hollowcube.mapmaker.player.DisplayName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LocalPlayerService extends NoopPlayerService {

    @Override
    public @NotNull DisplayName getPlayerDisplayName2(@NotNull String id) {
        var player = LocalServiceUtil.findPlayer(id);
        return new DisplayName(List.of(new DisplayName.Part("username", player.getUsername(), null)));
    }

}
