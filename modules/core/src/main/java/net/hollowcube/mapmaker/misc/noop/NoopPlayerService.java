package net.hollowcube.mapmaker.misc.noop;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.Hats;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.player.responses.SendFriendRequestResult;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class NoopPlayerService implements PlayerService {

    @Override
    public void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update) {

    }

    @Override
    public @NotNull Set<String> getUnlockedCosmetics(@NotNull String playerId) {
        return Set.of(Hats.HARD_HAT.path());
    }

    @Override
    public void buyCosmetic(
        @NotNull String id, @NotNull Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits,
        @Nullable JsonObject items
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void buyUpgrade(@NotNull String playerId, @NotNull String upgradeId, int cubits, @NotNull JsonObject meta) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull CreateCheckoutLinkResponse createCheckoutLink(
        @NotNull String source, @NotNull String playerId, @NotNull String productId) {
        return new CreateCheckoutLinkResponse("https://hollowcube.net/store/checkout/fkelnk");
    }

    @Override
    public @NotNull TotpResult checkTotp(@NotNull String playerId, @Nullable String code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull TotpResult removeTotp(@NotNull String playerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable TotpSetupResponse beginTotpSetup(@NotNull String playerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull TotpResult completeTotpSetup(@NotNull String playerId, @NotNull String code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Page<PlayerFriend> getPlayerFriends(@NotNull String playerId, @Nullable Boolean onlineState, @NotNull Pageable pageable) {
        return Page.empty();
    }

    @Override
    public void removeFriend(@NotNull String playerId, @NotNull String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Page<FriendRequest> getFriendRequests(@NotNull String playerId, boolean incoming, @NotNull Pageable pageable) {
        return Page.empty();
    }

    @Override
    public @NotNull SendFriendRequestResult sendFriendRequest(@NotNull String playerId, @NotNull String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull FriendRequest deleteFriendRequest(@NotNull String playerId, @NotNull String targetId, boolean bidirectional) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void blockPlayer(@NotNull String playerId, @NotNull String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Page<BlockedPlayer> getBlockedPlayers(@NotNull String playerId, @NotNull Pageable pageable) {
        return Page.empty();
    }

    @Override
    public void unblockPlayer(@NotNull String playerId, @NotNull String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<BlockedPlayer> getBlocksBetween(
        @NotNull String playerId, @NotNull String targetId, boolean bidirectional) {
        return List.of();
    }

}
