package net.hollowcube.mapmaker.misc.noop;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.player.*;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class NoopPlayerService implements PlayerService {
    @Override
    public @NotNull DisplayName getPlayerDisplayName2(@NotNull String id) {
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(id));
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
    public @NotNull JsonObject getPlayerBackpack(@NotNull String id) {
        return new JsonObject();
    }

    @Override
    public @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query) {
        return new TabCompleteResponse(MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .map(p -> new TabCompleteResponse.Entry(p.getUuid().toString(), p.getUsername()))
                .toList());
    }

    @Override
    public @NotNull CreateCheckoutLinkResponse createCheckoutLink(@NotNull String source, @NotNull String playerId, @NotNull String productId) {
        return new CreateCheckoutLinkResponse(new CreateCheckoutLinkResponse.Link(productId, "https://hollowcube.net/store/checkout/fkelnk"), null);
    }

    @Override
    public @NotNull CreateCheckoutLinkResponse createCheckoutLink(@NotNull String source, @NotNull String playerId, int cubits) {
        return new CreateCheckoutLinkResponse(
                new CreateCheckoutLinkResponse.Link("cubits_50", "https://hollowcube.net/store/checkout/fkelnk"),
                new CreateCheckoutLinkResponse.Link("cubits_105", "https://hollowcube.net/store/checkout/fkelnk")
        );
    }
}
