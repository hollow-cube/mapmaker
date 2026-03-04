package net.hollowcube.mapmaker.misc.noop;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.Hats;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.player.responses.PlayerAlts;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.player.responses.SendFriendRequestResult;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NoopPlayerService implements PlayerService {
    @Override
    public @NotNull DisplayName getPlayerDisplayName2(@NotNull String id) {
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(id));
        if (player != null) {
            return PlayerData.fromPlayer(player).displayName2();
        }

        if (id.equals("597481a0-02fb-441c-9188-c407bec05084"))
            return new DisplayName(List.of(new DisplayName.Part("username", "TestPlayer", null)));

        return new DisplayName(List.of(new DisplayName.Part("username", "Unknown", null)));
    }

    @Override
    public @NotNull String getPlayerId(@NotNull String idOrUsername) {
        if (idOrUsername.equals("TestPlayer")) {
            return "597481a0-02fb-441c-9188-c407bec05084";
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PlayerData getPlayerData(@NotNull String id) {
        throw new UnsupportedOperationException();
    }

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
    public @NotNull JsonObject getPlayerBackpack(@NotNull String id) {
        return new JsonObject();
    }

    @Override
    public @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query, int limit) {
        return new TabCompleteResponse(MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                                           .map(p -> new TabCompleteResponse.Entry(p.getUuid().toString(),
                                                                                   p.getUsername()))
                                           .toList());
    }

    @Override
    public @NotNull CreateCheckoutLinkResponse createCheckoutLink(
        @NotNull String source, @NotNull String playerId, @NotNull String productId) {
        return new CreateCheckoutLinkResponse("https://hollowcube.net/store/checkout/fkelnk");
    }

    @Override
    public @Nullable HypercubeStatus getHypercubeStatus(@NotNull String playerId) {
        return null;
    }

    @Override
    public @NotNull LinkResult attemptVerify(@NotNull String playerId, @NotNull String secret) {
        throw new UnsupportedOperationException("");
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
    public @NotNull List<PlayerAlts.Alt> getAlts(@NotNull String playerId) {
        return List.of();
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

    @Override
    public @NotNull PlayerNotificationResponse getNotifications(@NotNull String playerId, int page, boolean unread) {
        return new PlayerNotificationResponse(0, 0, List.of());
    }

    @Override
    public void deleteNotification(@NotNull String playerId, @NotNull String notificationId) {

    }

    @Override
    public void markNotificationRead(@NotNull String playerId, @NotNull String notificationId, boolean read) {

    }

    @Override
    public void createNotification(
        @NotNull String playerId, @NotNull String type, @NotNull String key, @Nullable JsonObject data, @Nullable Integer expiresInSeconds, boolean replaceUnread
    ) {

    }

    @Override
    public @Nullable String getRecap(@NotNull String playerId, int year) {
        throw new UnsupportedOperationException();
    }
}
