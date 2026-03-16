package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.responses.PlayerAlts;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.player.responses.SendFriendRequestResult;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@Blocking
public interface PlayerService {
    int DEFAULT_TAB_COMPLETE_LIMIT = 25;

    DisplayName getPlayerDisplayName2(String id);

    default List<DisplayName> getPlayerDisplayNames(List<String> ids) {
        return ids.stream() // todo probably makes sense to add a bulk endpoint
            .map(this::getPlayerDisplayName2)
            .toList();
    }

    String getPlayerId(String idOrUsername);

    PlayerData getPlayerData(String id);

    void updatePlayerData(String id, PlayerDataUpdateRequest update);

    /**
     * Returns the unlocked cosmetics for the given player by their path, eg `head/crown`.
     */
    Set<String> getUnlockedCosmetics(String playerId);

    void buyCosmetic(
        String id, Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits,
        @Nullable JsonObject items
    );

    void buyUpgrade(String playerId, String upgradeId, int cubits, JsonObject meta);

    JsonObject getPlayerBackpack(String id);

    TabCompleteResponse getUsernameTabCompletions(String query, int limit);

    default TabCompleteResponse getUsernameTabCompletions(String query) {
        return this.getUsernameTabCompletions(query, DEFAULT_TAB_COMPLETE_LIMIT);
    }

    record CreateCheckoutLinkResponse(String url) {
    }

    /**
     * Creates a new checkout link for the given product.
     *
     * @param source   The source of the checkout link, eg "ingame/store" for the in-game store gui.
     * @param username The username of the player
     * @param product  The (internal) ID of the product, eg cubits_50
     */
    CreateCheckoutLinkResponse createCheckoutLink(String source, String username, String product);

    @Nullable HypercubeStatus getHypercubeStatus(String playerId);

    enum TotpResult {
        SUCCESS,
        INVALID_FORMAT,
        INVALID_CODE,
        NOT_ENABLED,
        ALREADY_ENABLED,
    }

    TotpResult checkTotp(String playerId, @Nullable String code);

    TotpResult removeTotp(String playerId);

    @Nullable TotpSetupResponse beginTotpSetup(String playerId);

    TotpResult completeTotpSetup(String playerId, String code);

    List<PlayerAlts.Alt> getAlts(String playerId);

    // Friendships

    default Page<PlayerFriend> getPlayerFriends(String playerId, Pageable pageable) {
        return getPlayerFriends(playerId, null, pageable);
    }

    Page<PlayerFriend> getPlayerFriends(String playerId, @Nullable Boolean onlineState, Pageable pageable);

    void removeFriend(String playerId, String targetId);

    Page<FriendRequest> getFriendRequests(String playerId, boolean incoming, Pageable pageable);

    /**
     * Sends a friend request or accepts a friend request if an inverted request already exists (one from the target)
     *
     * @param playerId player that is sending the request
     * @param targetId player that is receiving the request
     */
    SendFriendRequestResult sendFriendRequest(String playerId, String targetId);

    FriendRequest deleteFriendRequest(String playerId, String targetId, boolean bidirectional);

    // Blocks

    void blockPlayer(String playerId, String targetId);

    Page<BlockedPlayer> getBlockedPlayers(String playerId, Pageable pageable);

    void unblockPlayer(String playerId, String targetId);

    List<BlockedPlayer> getBlocksBetween(String playerId, String targetId, boolean bidirectional);

    /**
     *
     * @param player        player to test the block for
     * @param targetId      target to test the block against
     * @param bidirectional if bidirectional or not
     * @return true if the player is blocked by the target - you should stop execution
     */
    default boolean failIfBlocked(Player player, String targetId, String targetUsername, boolean bidirectional) {
        var blocks = this.getBlocksBetween(targetId, player.getUuid().toString(), bidirectional);
        if (blocks.isEmpty()) return false;

        BlockedPlayer block = blocks.getFirst();
        if (block.playerId().equals(player.getUuid().toString())) { // blocked by target
            player.sendMessage(
                Component.translatable("generic.command.blocked_by_target", Component.text(targetUsername)));
        } else { // blocked by self
            player.sendMessage(Component.translatable("generic.command.blocked_by_self", Component.text(targetUsername)));
        }
        return true;
    }

    // Notifications
    PlayerNotificationResponse getNotifications(String playerId, int page, boolean unread);

    void deleteNotification(String playerId, String notificationId);

    void markNotificationRead(String playerId, String notificationId, boolean read);

    void createNotification(
        String playerId, String type, String key, @Nullable JsonObject data,
        @Nullable Integer expiresInSeconds, boolean replaceUnread
    );

    // Recap

    @Nullable String getRecap(String playerId, int year);

    class BadRequestError extends RuntimeException {}

    class NotFoundError extends RuntimeException {

    }

    class AlreadyExistsError extends RuntimeException {

    }

    record Pageable(int page, int pageSize) {}

    @RuntimeGson
    record Page<T>(int page, int totalItems, List<T> items) {

        public static <T> Page<T> fromJson(String json, Class<T> clazz) {
            return AbstractHttpService.GSON.fromJson(json, TypeToken.getParameterized(Page.class, clazz).getType());
        }

        public static <T> Page<T> empty() {
            return new Page<>(0, 0, List.of());
        }

    }

}
