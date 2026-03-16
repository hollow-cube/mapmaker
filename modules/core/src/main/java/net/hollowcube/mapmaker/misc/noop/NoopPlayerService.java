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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NoopPlayerService implements PlayerService {
    @Override
    public DisplayName getPlayerDisplayName2(String id) {
        var player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(id));
        if (player != null) {
            return PlayerData.fromPlayer(player).displayName2();
        }

        if (id.equals("597481a0-02fb-441c-9188-c407bec05084"))
            return new DisplayName(List.of(new DisplayName.Part("username", "TestPlayer", null)));

        return new DisplayName(List.of(new DisplayName.Part("username", "Unknown", null)));
    }

    @Override
    public String getPlayerId(String idOrUsername) {
        if (idOrUsername.equals("TestPlayer")) {
            return "597481a0-02fb-441c-9188-c407bec05084";
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public PlayerData getPlayerData(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updatePlayerData(String id, PlayerDataUpdateRequest update) {

    }

    @Override
    public Set<String> getUnlockedCosmetics(String playerId) {
        return Set.of(Hats.HARD_HAT.path());
    }

    @Override
    public void buyCosmetic(
        String id, Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits,
        @Nullable JsonObject items
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void buyUpgrade(String playerId, String upgradeId, int cubits, JsonObject meta) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject getPlayerBackpack(String id) {
        return new JsonObject();
    }

    @Override
    public TabCompleteResponse getUsernameTabCompletions(String query, int limit) {
        return new TabCompleteResponse(MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
            .map(p -> new TabCompleteResponse.Entry(p.getUuid().toString(),
                p.getUsername()))
            .toList());
    }

    @Override
    public CreateCheckoutLinkResponse createCheckoutLink(String source, String playerId, String productId) {
        return new CreateCheckoutLinkResponse("https://hollowcube.net/store/checkout/fkelnk");
    }

    @Override
    public @Nullable HypercubeStatus getHypercubeStatus(String playerId) {
        return null;
    }

    @Override
    public TotpResult checkTotp(String playerId, @Nullable String code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TotpResult removeTotp(String playerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable TotpSetupResponse beginTotpSetup(String playerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TotpResult completeTotpSetup(String playerId, String code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PlayerAlts.Alt> getAlts(String playerId) {
        return List.of();
    }

    @Override
    public Page<PlayerFriend> getPlayerFriends(String playerId, @Nullable Boolean onlineState, Pageable pageable) {
        return Page.empty();
    }

    @Override
    public void removeFriend(String playerId, String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<FriendRequest> getFriendRequests(String playerId, boolean incoming, Pageable pageable) {
        return Page.empty();
    }

    @Override
    public SendFriendRequestResult sendFriendRequest(String playerId, String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FriendRequest deleteFriendRequest(String playerId, String targetId, boolean bidirectional) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void blockPlayer(String playerId, String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<BlockedPlayer> getBlockedPlayers(String playerId, Pageable pageable) {
        return Page.empty();
    }

    @Override
    public void unblockPlayer(String playerId, String targetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<BlockedPlayer> getBlocksBetween(String playerId, String targetId, boolean bidirectional) {
        return List.of();
    }

    @Override
    public PlayerNotificationResponse getNotifications(String playerId, int page, boolean unread) {
        return new PlayerNotificationResponse(0, 0, List.of());
    }

    @Override
    public void deleteNotification(String playerId, String notificationId) {

    }

    @Override
    public void markNotificationRead(String playerId, String notificationId, boolean read) {

    }

    @Override
    public void createNotification(
        String playerId, String type, String key, @Nullable JsonObject data, @Nullable Integer expiresInSeconds, boolean replaceUnread
    ) {

    }

    @Override
    public @Nullable String getRecap(String playerId, int year) {
        throw new UnsupportedOperationException();
    }
}
