package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.player.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.ConnectionState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NoopPlayerService implements PlayerService {
    @Override
    public @NotNull DisplayName getPlayerDisplayName2(@NotNull String id) {
        var player = MinecraftServer.getConnectionManager().getPlayer(id);
        if (player != null) {
            return PlayerDataV2.fromPlayer(player).displayName2();
        }

        if (id.equals("597481a0-02fb-441c-9188-c407bec05084"))
            return new DisplayName(List.of(new DisplayName.Part("username", "TestPlayer", null)));

        throw new NotFoundError();
    }

    @Override
    public @NotNull String getPlayerId(@NotNull String idOrUsername) {
        if (idOrUsername.equals("TestPlayer")) {
            return "597481a0-02fb-441c-9188-c407bec05084";
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update) {

    }

    @Override
    public @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query) {
        return new TabCompleteResponse(MinecraftServer.getConnectionManager().getPlayers(ConnectionState.PLAY).stream()
                .map(p -> new TabCompleteResponse.Entry(p.getUuid().toString(), p.getUsername()))
                .toList());
    }
}
